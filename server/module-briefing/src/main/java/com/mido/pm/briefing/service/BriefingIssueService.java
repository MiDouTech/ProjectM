package com.mido.pm.briefing.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.briefing.dto.IssueCreateDTO;
import com.mido.pm.briefing.dto.IssueVO;
import com.mido.pm.briefing.entity.PmBriefing;
import com.mido.pm.briefing.entity.PmBriefingIssue;
import com.mido.pm.briefing.event.BriefingEvents;
import com.mido.pm.briefing.mapper.PmBriefingIssueMapper;
import com.mido.pm.briefing.mapper.PmBriefingMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 简报跟进问题：从可见简报(作者或评审人)提出问题、指派负责人、跟踪状态。
 * 「跟进的问题」= 我提出的或我负责的。
 */
@Service
public class BriefingIssueService {

    private static final Set<String> STATUSES = Set.of("open", "following", "closed");

    private final PmBriefingIssueMapper issueMapper;
    private final PmBriefingMapper briefingMapper;
    private final BriefingReviewService reviewService;
    private final DomainEventPublisher eventPublisher;

    public BriefingIssueService(PmBriefingIssueMapper issueMapper, PmBriefingMapper briefingMapper,
                                BriefingReviewService reviewService, DomainEventPublisher eventPublisher) {
        this.issueMapper = issueMapper;
        this.briefingMapper = briefingMapper;
        this.reviewService = reviewService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(IssueCreateDTO dto) {
        PmBriefing b = briefingMapper.selectById(dto.briefingId());
        if (b == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "简报不存在");
        }
        Long me = currentUserId();
        if (!me.equals(b.getAuthorId()) && !reviewService.isReviewer(dto.briefingId(), me)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权对该简报提问题");
        }
        PmBriefingIssue issue = new PmBriefingIssue();
        issue.setBriefingId(dto.briefingId());
        issue.setRaisedBy(me);
        issue.setOwnerId(dto.ownerId() == null ? me : dto.ownerId());
        issue.setContent(dto.content());
        issue.setStatus("open");
        issue.setDueDate(dto.dueDate());
        issueMapper.insert(issue);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("issueId", issue.getId());
        payload.put("briefingId", issue.getBriefingId());
        payload.put("raisedBy", me);
        payload.put("ownerId", issue.getOwnerId());
        payload.put("occurredAt", LocalDateTime.now().toString());
        eventPublisher.publish(BriefingEvents.ISSUE_RAISED, payload);
        return issue.getId();
    }

    /** 跟进的问题：我提出的或我负责的（可按状态过滤）。 */
    public List<IssueVO> listMine(String status) {
        Long me = currentUserId();
        return issueMapper.selectList(Wrappers.<PmBriefingIssue>lambdaQuery()
                        .and(w -> w.eq(PmBriefingIssue::getRaisedBy, me).or().eq(PmBriefingIssue::getOwnerId, me))
                        .eq(status != null && !status.isBlank(), PmBriefingIssue::getStatus, status)
                        .orderByDesc(PmBriefingIssue::getId))
                .stream().map(this::toVO).toList();
    }

    /** 更新状态（仅提出人或负责人）。关闭时发 briefing.issue.closed。 */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        if (!STATUSES.contains(status)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "状态非法");
        }
        PmBriefingIssue issue = issueMapper.selectById(id);
        if (issue == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "问题不存在");
        }
        Long me = currentUserId();
        if (!me.equals(issue.getRaisedBy()) && !me.equals(issue.getOwnerId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该问题");
        }
        issue.setStatus(status);
        issueMapper.updateById(issue);

        if ("closed".equals(status)) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("issueId", id);
            payload.put("briefingId", issue.getBriefingId());
            payload.put("operatorId", me);
            payload.put("occurredAt", LocalDateTime.now().toString());
            eventPublisher.publish(BriefingEvents.ISSUE_CLOSED, payload);
        }
    }

    private IssueVO toVO(PmBriefingIssue i) {
        return new IssueVO(i.getId(), i.getBriefingId(), i.getRaisedBy(), i.getOwnerId(),
                i.getContent(), i.getStatus(), i.getDueDate());
    }

    private Long currentUserId() {
        return UserContext.currentUserId();
    }
}
