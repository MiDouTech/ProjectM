package com.mido.pm.briefing.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.briefing.dto.BriefingSaveDTO;
import com.mido.pm.briefing.dto.BriefingVO;
import com.mido.pm.briefing.entity.PmBriefing;
import com.mido.pm.briefing.entity.PmBriefingTemplate;
import com.mido.pm.briefing.event.BriefingEvents;
import com.mido.pm.briefing.mapper.PmBriefingMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 简报服务：按「模板+周期」幂等保存草稿、提交、查看我的简报。
 * 提交发 briefing.submitted（订阅方见 docs/domain-events.md）。评审/统计为 P1/P2。
 */
@Service
public class BriefingService {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_SUBMITTED = "submitted";

    private final PmBriefingMapper briefingMapper;
    private final BriefingTemplateService templateService;
    private final BriefingReviewService reviewService;
    private final DomainEventPublisher eventPublisher;

    public BriefingService(PmBriefingMapper briefingMapper, BriefingTemplateService templateService,
                           BriefingReviewService reviewService, DomainEventPublisher eventPublisher) {
        this.briefingMapper = briefingMapper;
        this.templateService = templateService;
        this.reviewService = reviewService;
        this.eventPublisher = eventPublisher;
    }

    /** 保存草稿（按 当前用户+模板+周期 幂等 upsert）。已提交的不可再改。 */
    @Transactional(rollbackFor = Exception.class)
    public Long save(BriefingSaveDTO dto) {
        PmBriefingTemplate template = templateService.require(dto.templateId());
        Long me = currentUserId();
        String contentJson = JSONUtil.toJsonStr(dto.content() == null ? Map.of() : dto.content());

        PmBriefing existing = briefingMapper.selectOne(Wrappers.<PmBriefing>lambdaQuery()
                .eq(PmBriefing::getAuthorId, me)
                .eq(PmBriefing::getTemplateId, dto.templateId())
                .eq(PmBriefing::getPeriodKey, dto.periodKey())
                .last("limit 1"));
        if (existing != null) {
            if (STATUS_SUBMITTED.equals(existing.getStatus())) {
                throw new BizException(ErrorCode.CONFLICT, "该简报已提交，不可修改");
            }
            existing.setContent(contentJson);
            existing.setPeriodStart(dto.periodStart());
            existing.setPeriodEnd(dto.periodEnd());
            briefingMapper.updateById(existing);
            return existing.getId();
        }
        PmBriefing b = new PmBriefing();
        b.setTemplateId(dto.templateId());
        b.setType(template.getType());
        b.setAuthorId(me);
        b.setDeptId(currentDeptId());
        b.setPeriodKey(dto.periodKey());
        b.setPeriodStart(dto.periodStart());
        b.setPeriodEnd(dto.periodEnd());
        b.setContent(contentJson);
        b.setStatus(STATUS_DRAFT);
        briefingMapper.insert(b);
        return b.getId();
    }

    /** 提交简报（仅作者，幂等：已提交再次提交报冲突）。 */
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        PmBriefing b = requireOwn(id);
        if (STATUS_SUBMITTED.equals(b.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "简报已提交");
        }
        b.setStatus(STATUS_SUBMITTED);
        b.setSubmittedAt(LocalDateTime.now());
        briefingMapper.updateById(b);
        Long reviewerId = reviewService.assignReviewer(b);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("briefingId", b.getId());
        payload.put("type", b.getType());
        payload.put("authorId", b.getAuthorId());
        payload.put("periodKey", b.getPeriodKey());
        payload.put("reviewerIds", reviewerId == null ? List.of() : List.of(reviewerId));
        payload.put("occurredAt", LocalDateTime.now().toString());
        eventPublisher.publish(BriefingEvents.SUBMITTED, payload);
    }

    /** 我的简报（按类型，最新在前）。 */
    public List<BriefingVO> listMine(String type) {
        return briefingMapper.selectList(Wrappers.<PmBriefing>lambdaQuery()
                        .eq(PmBriefing::getAuthorId, currentUserId())
                        .eq(type != null && !type.isBlank(), PmBriefing::getType, type)
                        .orderByDesc(PmBriefing::getId))
                .stream().map(this::toVO).toList();
    }

    /** 简报详情：作者或评审人可见。 */
    public BriefingVO get(Long id) {
        PmBriefing b = briefingMapper.selectById(id);
        if (b == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "简报不存在");
        }
        Long me = currentUserId();
        if (!me.equals(b.getAuthorId()) && !reviewService.isReviewer(id, me)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该简报");
        }
        return toVO(b);
    }

    private PmBriefing requireOwn(Long id) {
        PmBriefing b = briefingMapper.selectById(id);
        if (b == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "简报不存在");
        }
        if (!currentUserId().equals(b.getAuthorId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该简报");
        }
        return b;
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

    private Long currentDeptId() {
        CurrentUser u = UserContext.get();
        return u == null ? null : u.getDeptId();
    }
}
