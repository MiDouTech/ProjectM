package com.mido.pm.verify.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.project.domain.ProjectStatus;
import com.mido.pm.project.dto.ProjectTransitionDTO;
import com.mido.pm.project.service.ProjectService;
import com.mido.pm.stakeholder.dto.StakeholderVO;
import com.mido.pm.stakeholder.service.StakeholderService;
import com.mido.pm.verify.domain.NpssCalculator;
import com.mido.pm.verify.domain.NpssCalculator.ScoreWeight;
import com.mido.pm.verify.domain.ResultLevel;
import com.mido.pm.verify.dto.NpssReviewVO;
import com.mido.pm.verify.dto.NpssScoreVO;
import com.mido.pm.verify.dto.ScoreSubmitDTO;
import com.mido.pm.verify.entity.PmNpssReview;
import com.mido.pm.verify.entity.PmNpssScore;
import com.mido.pm.verify.event.NpssEvents;
import com.mido.pm.verify.mapper.PmNpssReviewMapper;
import com.mido.pm.verify.mapper.PmNpssScoreMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NPSS 价值验收编排（npss-rule §2/§3）：发起轮次（定时 Job 调用）、干系人评分（幂等）、汇总分级。
 * 写操作发 npss.review.started / npss.scored / npss.review.completed。
 */
@Service
public class NpssReviewService {

    /** 轮次状态 */
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_DONE = "done";

    private final PmNpssReviewMapper reviewMapper;
    private final PmNpssScoreMapper scoreMapper;
    private final ProjectService projectService;
    private final StakeholderService stakeholderService;
    private final DomainEventPublisher eventPublisher;

    public NpssReviewService(PmNpssReviewMapper reviewMapper, PmNpssScoreMapper scoreMapper,
                             ProjectService projectService, StakeholderService stakeholderService,
                             DomainEventPublisher eventPublisher) {
        this.reviewMapper = reviewMapper;
        this.scoreMapper = scoreMapper;
        this.projectService = projectService;
        this.stakeholderService = stakeholderService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 发起价值验收（npss-rule §2）：项目 已结案→价值验收中 → 建 review(pending) → 为每个干系人建评分待办
     * → 发 npss.review.started（携带收件干系人 userId，由通知监听器多通道通知，含企微）。
     * 不在此直接推送：业务不直连消息通道，统一经领域事件 + NotificationListener 路由。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long startReview(Long projectId) {
        // 重入幂等（npss-rule §2 延后触发）：已有进行中轮次则不重复发起（防定时 Job 重复扫描建重复 review）
        List<PmNpssReview> pending = reviewMapper.selectList(Wrappers.<PmNpssReview>lambdaQuery()
                .eq(PmNpssReview::getProjectId, projectId)
                .eq(PmNpssReview::getStatus, STATUS_PENDING));
        if (!pending.isEmpty()) {
            return pending.get(0).getId();
        }
        // 状态机：已结案 → 价值验收中（非法流转将由项目状态机拒绝）
        projectService.transition(projectId,
                new ProjectTransitionDTO(ProjectStatus.VALUE_VERIFY.getCode(), null));

        PmNpssReview review = new PmNpssReview();
        review.setProjectId(projectId);
        review.setRound("1");
        review.setStatus(STATUS_PENDING);
        reviewMapper.insert(review);

        List<StakeholderVO> stakeholders = stakeholderService.list(projectId);
        List<Long> recipientUserIds = new ArrayList<>();
        for (StakeholderVO s : stakeholders) {
            PmNpssScore todo = new PmNpssScore();
            todo.setReviewId(review.getId());
            todo.setStakeholderId(s.id());
            todo.setWeight(s.npssWeight());
            scoreMapper.insert(todo); // score 留空=待打分
            if (s.userId() != null) {
                recipientUserIds.add(s.userId());
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reviewId", review.getId());
        payload.put("projectId", projectId);
        payload.put("stakeholderCount", stakeholders.size());
        payload.put("recipientUserIds", recipientUserIds); // 收件干系人，监听器据此多通道通知
        eventPublisher.publish(NpssEvents.REVIEW_STARTED, payload);
        return review.getId();
    }

    /**
     * 干系人提交评分。幂等（防重复打分）：同一 (review, stakeholder) 已打分则原样返回，不重复写入/发事件。
     * 全部干系人打分完成后自动汇总。
     */
    @Transactional(rollbackFor = Exception.class)
    public NpssScoreVO submitScore(Long reviewId, ScoreSubmitDTO dto) {
        PmNpssReview review = requireReview(reviewId);
        if (!STATUS_PENDING.equals(review.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "该轮次评分已结束");
        }
        PmNpssScore row = scoreMapper.selectOne(Wrappers.<PmNpssScore>lambdaQuery()
                .eq(PmNpssScore::getReviewId, reviewId)
                .eq(PmNpssScore::getStakeholderId, dto.stakeholderId()));
        if (row == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该干系人不在本轮评分名单");
        }
        if (row.getScore() != null) {
            return toVO(row); // 幂等：已打分，原样返回
        }
        row.setScore(dto.score());
        row.setComment(dto.comment());
        scoreMapper.updateById(row);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reviewId", reviewId);
        payload.put("stakeholderId", dto.stakeholderId());
        payload.put("score", dto.score());
        eventPublisher.publish(NpssEvents.SCORED, payload);

        if (allScored(reviewId)) {
            summarize(review);
        }
        return toVO(row);
    }

    /** 汇总分级（npss-rule §3）：算加权满意度 → 分级 → 写 review → 发 npss.review.completed。 */
    @Transactional(rollbackFor = Exception.class)
    public void summarize(PmNpssReview review) {
        List<ScoreWeight> scores = scoreMapper.selectList(Wrappers.<PmNpssScore>lambdaQuery()
                        .eq(PmNpssScore::getReviewId, review.getId()))
                .stream()
                .filter(s -> s.getScore() != null)
                .map(s -> new ScoreWeight(s.getScore(), s.getWeight()))
                .toList();
        java.math.BigDecimal weighted = NpssCalculator.weightedSatisfaction(scores);
        ResultLevel level = NpssCalculator.level(weighted);

        review.setWeightedScore(weighted);
        review.setResultLevel(level.getCode());
        review.setStatus(STATUS_DONE);
        review.setReviewedAt(LocalDateTime.now());
        reviewMapper.updateById(review);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reviewId", review.getId());
        payload.put("projectId", review.getProjectId());
        payload.put("weightedScore", weighted);
        payload.put("resultLevel", level.getCode());
        eventPublisher.publish(NpssEvents.REVIEW_COMPLETED, payload);
    }

    /** 某项目的 NPSS 轮次（新→旧），供项目详情"验收"Tab 展示。 */
    public List<NpssReviewVO> listByProject(Long projectId) {
        return reviewMapper.selectList(Wrappers.<PmNpssReview>lambdaQuery()
                        .eq(PmNpssReview::getProjectId, projectId)
                        .orderByDesc(PmNpssReview::getId))
                .stream().map(r -> get(r.getId())).toList();
    }

    /**
     * 已汇总轮次按 result_level 计数（reviewed_at ∈ [from, toExclusive)）。供报表域 PMO 总体评价聚合（npss-rule §5）。
     */
    public Map<String, Long> levelCounts(java.time.LocalDateTime from, java.time.LocalDateTime toExclusive) {
        Map<String, Long> counts = new LinkedHashMap<>();
        reviewMapper.selectList(Wrappers.<PmNpssReview>lambdaQuery()
                        .eq(PmNpssReview::getStatus, STATUS_DONE)
                        .ge(PmNpssReview::getReviewedAt, from)
                        .lt(PmNpssReview::getReviewedAt, toExclusive))
                .forEach(r -> counts.merge(r.getResultLevel(), 1L, Long::sum));
        return counts;
    }

    public NpssReviewVO get(Long reviewId) {
        PmNpssReview r = requireReview(reviewId);
        List<NpssScoreVO> scores = scoreMapper.selectList(Wrappers.<PmNpssScore>lambdaQuery()
                        .eq(PmNpssScore::getReviewId, reviewId))
                .stream().map(this::toVO).toList();
        return new NpssReviewVO(r.getId(), r.getProjectId(), r.getRound(), r.getStatus(),
                r.getWeightedScore(), r.getResultLevel(), r.getReviewedAt(), scores);
    }

    private boolean allScored(Long reviewId) {
        return scoreMapper.selectCount(Wrappers.<PmNpssScore>lambdaQuery()
                .eq(PmNpssScore::getReviewId, reviewId)
                .isNull(PmNpssScore::getScore)) == 0;
    }

    private PmNpssReview requireReview(Long id) {
        PmNpssReview r = reviewMapper.selectById(id);
        if (r == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "评分轮次不存在");
        }
        return r;
    }

    private NpssScoreVO toVO(PmNpssScore s) {
        return new NpssScoreVO(s.getId(), s.getReviewId(), s.getStakeholderId(),
                s.getScore(), s.getWeight(), s.getComment());
    }
}
