package com.mido.pm.briefing.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.briefing.dto.BriefingReviewDTO;
import com.mido.pm.briefing.dto.BriefingReviewVO;
import com.mido.pm.briefing.dto.BriefingVO;
import com.mido.pm.briefing.entity.PmBriefing;
import com.mido.pm.briefing.entity.PmBriefingRecipient;
import com.mido.pm.briefing.entity.PmBriefingReview;
import com.mido.pm.briefing.event.BriefingEvents;
import com.mido.pm.briefing.mapper.PmBriefingMapper;
import com.mido.pm.briefing.mapper.PmBriefingRecipientMapper;
import com.mido.pm.briefing.mapper.PmBriefingReviewMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.org.service.SysDeptService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 简报评审服务：提交时按作者部门负责人落评审人；评审人批注；评审人侧「我评审的」「成员简报」查询。
 */
@Service
public class BriefingReviewService {

    private static final String TYPE_REVIEWER = "reviewer";
    private static final String STATUS_SUBMITTED = "submitted";

    private final PmBriefingMapper briefingMapper;
    private final PmBriefingRecipientMapper recipientMapper;
    private final PmBriefingReviewMapper reviewMapper;
    private final SysDeptService deptService;
    private final DomainEventPublisher eventPublisher;

    public BriefingReviewService(PmBriefingMapper briefingMapper, PmBriefingRecipientMapper recipientMapper,
                                 PmBriefingReviewMapper reviewMapper, SysDeptService deptService,
                                 DomainEventPublisher eventPublisher) {
        this.briefingMapper = briefingMapper;
        this.recipientMapper = recipientMapper;
        this.reviewMapper = reviewMapper;
        this.deptService = deptService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 提交时落评审人=作者部门负责人（非作者本人、且未重复）。
     *
     * @return 评审人 id；无负责人/即作者本人时返回 null
     */
    @Transactional(rollbackFor = Exception.class)
    public Long assignReviewer(PmBriefing b) {
        Long reviewer = deptService.leaderOf(b.getDeptId());
        if (reviewer == null || reviewer.equals(b.getAuthorId())) {
            return null;
        }
        Long exists = recipientMapper.selectCount(Wrappers.<PmBriefingRecipient>lambdaQuery()
                .eq(PmBriefingRecipient::getBriefingId, b.getId())
                .eq(PmBriefingRecipient::getUserId, reviewer)
                .eq(PmBriefingRecipient::getType, TYPE_REVIEWER));
        if (exists != null && exists > 0) {
            return reviewer;
        }
        PmBriefingRecipient r = new PmBriefingRecipient();
        r.setBriefingId(b.getId());
        r.setUserId(reviewer);
        r.setType(TYPE_REVIEWER);
        recipientMapper.insert(r);
        return reviewer;
    }

    public boolean isReviewer(Long briefingId, Long userId) {
        Long count = recipientMapper.selectCount(Wrappers.<PmBriefingRecipient>lambdaQuery()
                .eq(PmBriefingRecipient::getBriefingId, briefingId)
                .eq(PmBriefingRecipient::getUserId, userId)
                .eq(PmBriefingRecipient::getType, TYPE_REVIEWER));
        return count != null && count > 0;
    }

    /** 评审人对简报批注。 */
    @Transactional(rollbackFor = Exception.class)
    public Long addReview(Long briefingId, BriefingReviewDTO dto) {
        PmBriefing b = briefingMapper.selectById(briefingId);
        if (b == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "简报不存在");
        }
        Long me = currentUserId();
        if (!isReviewer(briefingId, me)) {
            throw new BizException(ErrorCode.FORBIDDEN, "非评审人不可批注");
        }
        PmBriefingReview r = new PmBriefingReview();
        r.setBriefingId(briefingId);
        r.setReviewerId(me);
        r.setAction("approve".equals(dto.action()) ? "approve" : "comment");
        r.setComment(dto.comment());
        r.setReviewedAt(LocalDateTime.now());
        reviewMapper.insert(r);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("briefingId", briefingId);
        payload.put("reviewerId", me);
        payload.put("authorId", b.getAuthorId());
        payload.put("action", r.getAction());
        payload.put("occurredAt", LocalDateTime.now().toString());
        eventPublisher.publish(BriefingEvents.REVIEWED, payload);
        return r.getId();
    }

    /** 简报的评审批注列表（作者或评审人可见）。 */
    public List<BriefingReviewVO> listReviews(Long briefingId) {
        PmBriefing b = briefingMapper.selectById(briefingId);
        if (b == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "简报不存在");
        }
        Long me = currentUserId();
        if (!me.equals(b.getAuthorId()) && !isReviewer(briefingId, me)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看评审");
        }
        return reviewMapper.selectList(Wrappers.<PmBriefingReview>lambdaQuery()
                        .eq(PmBriefingReview::getBriefingId, briefingId)
                        .orderByAsc(PmBriefingReview::getId))
                .stream().map(r -> new BriefingReviewVO(r.getId(), r.getReviewerId(),
                        r.getAction(), r.getComment(), r.getReviewedAt()))
                .toList();
    }

    /** 我评审的 / 成员简报：当前用户作为评审人的已提交简报（可按类型、作者过滤）。 */
    public List<BriefingVO> reviewBriefings(String type, Long authorId) {
        List<Long> ids = myReviewBriefingIds();
        if (ids.isEmpty()) {
            return List.of();
        }
        return briefingMapper.selectList(Wrappers.<PmBriefing>lambdaQuery()
                        .in(PmBriefing::getId, ids)
                        .eq(PmBriefing::getStatus, STATUS_SUBMITTED)
                        .eq(type != null && !type.isBlank(), PmBriefing::getType, type)
                        .eq(authorId != null, PmBriefing::getAuthorId, authorId)
                        .orderByDesc(PmBriefing::getId))
                .stream().map(this::toVO).toList();
    }

    /** 我评审范围内的成员（作者 id 去重）。 */
    public List<Long> reviewees() {
        List<Long> ids = myReviewBriefingIds();
        if (ids.isEmpty()) {
            return List.of();
        }
        return briefingMapper.selectList(Wrappers.<PmBriefing>lambdaQuery()
                        .select(PmBriefing::getAuthorId)
                        .in(PmBriefing::getId, ids)
                        .eq(PmBriefing::getStatus, STATUS_SUBMITTED))
                .stream().map(PmBriefing::getAuthorId).distinct().toList();
    }

    private List<Long> myReviewBriefingIds() {
        return recipientMapper.selectList(Wrappers.<PmBriefingRecipient>lambdaQuery()
                        .select(PmBriefingRecipient::getBriefingId)
                        .eq(PmBriefingRecipient::getUserId, currentUserId())
                        .eq(PmBriefingRecipient::getType, TYPE_REVIEWER))
                .stream().map(PmBriefingRecipient::getBriefingId).toList();
    }

    @SuppressWarnings("unchecked")
    private BriefingVO toVO(PmBriefing b) {
        Map<String, Object> content = b.getContent() == null || b.getContent().isBlank()
                ? Map.of() : JSONUtil.toBean(b.getContent(), Map.class);
        return new BriefingVO(b.getId(), b.getTemplateId(), b.getType(), b.getAuthorId(),
                b.getPeriodKey(), b.getPeriodStart(), b.getPeriodEnd(), content,
                b.getStatus(), b.getSubmittedAt());
    }

    private Long currentUserId() {
        return UserContext.currentUserId();
    }
}
