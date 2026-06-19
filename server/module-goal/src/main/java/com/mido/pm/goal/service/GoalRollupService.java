package com.mido.pm.goal.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.goal.domain.GoalProgress;
import com.mido.pm.goal.domain.ProjectCompletionPort;
import com.mido.pm.goal.dto.GoalContributionVO;
import com.mido.pm.goal.entity.PmGoal;
import com.mido.pm.goal.entity.PmGoalAlignment;
import com.mido.pm.goal.event.GoalEvents;
import com.mido.pm.goal.mapper.PmGoalAlignmentMapper;
import com.mido.pm.goal.mapper.PmGoalMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 目标进度自动汇总（G1）：项目/任务进度变化时，把"对齐项目的任务完成率"按对齐权重加权，
 * 反写到开启 auto_rollup 的量化 KR（current = start + 加权完成率% × (target-start)），并发 goal.progress.changed。
 * 单向（项目→KR），不回写项目，杜绝环。完成率口径由 {@link ProjectCompletionPort}（任务域实现）提供。
 */
@Service
public class GoalRollupService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PmGoalMapper goalMapper;
    private final PmGoalAlignmentMapper alignmentMapper;
    private final ProjectCompletionPort completionPort;
    private final DomainEventPublisher eventPublisher;

    public GoalRollupService(PmGoalMapper goalMapper, PmGoalAlignmentMapper alignmentMapper,
                             ProjectCompletionPort completionPort, DomainEventPublisher eventPublisher) {
        this.goalMapper = goalMapper;
        this.alignmentMapper = alignmentMapper;
        this.completionPort = completionPort;
        this.eventPublisher = eventPublisher;
    }

    /** 某项目进度变化 → 重算所有对齐到它且开启自动汇总的 KR。 */
    @Transactional(rollbackFor = Exception.class)
    public void recomputeForProject(Long projectId) {
        if (projectId == null) {
            return;
        }
        // 对齐到该项目的目标 id（去重）
        List<PmGoalAlignment> hits = alignmentMapper.selectList(Wrappers.<PmGoalAlignment>lambdaQuery()
                .eq(PmGoalAlignment::getTargetType, "project")
                .eq(PmGoalAlignment::getTargetId, projectId));
        for (Long goalId : hits.stream().map(PmGoalAlignment::getGoalId).distinct().toList()) {
            recomputeGoal(goalId);
        }
    }

    /** 重算单个 KR（仅开启 auto_rollup 时反写）。对齐权重调整后亦走此。 */
    @Transactional(rollbackFor = Exception.class)
    public void recomputeGoal(Long goalId) {
        PmGoal g = goalMapper.selectById(goalId);
        if (g == null || !Integer.valueOf(1).equals(g.getAutoRollup())) {
            return; // 仅开启 auto_rollup 的 KR 自动反写
        }
        Breakdown b = breakdown(goalId);
        if (b.weightSum.signum() <= 0) {
            return; // 无有效对齐项目，不动 KR
        }
        // 映射到 KR 量纲：current = start + 加权完成率% × (target-start)
        BigDecimal start = g.getMetricStart();
        BigDecimal target = g.getMetricTarget();
        if (start != null && target != null && target.compareTo(start) != 0) {
            BigDecimal current = start.add(
                    b.weightedRate.divide(HUNDRED, 6, RoundingMode.HALF_UP).multiply(target.subtract(start)));
            g.setMetricCurrent(current.setScale(2, RoundingMode.HALF_UP));
        }
        g.setProgress(GoalProgress.compute(start, target, g.getMetricCurrent()));
        goalMapper.updateById(g);
        eventPublisher.publish(GoalEvents.PROGRESS_CHANGED, progressPayload(g));
    }

    /** 反向贡献度看板：某 KR 各对齐项目的完成率/权重/贡献（只读，不反写）。 */
    public GoalContributionVO contribution(Long goalId) {
        PmGoal g = goalMapper.selectById(goalId);
        if (g == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "目标不存在: " + goalId);
        }
        Breakdown b = breakdown(goalId);
        List<GoalContributionVO.Item> items = new ArrayList<>();
        for (Line line : b.lines) {
            BigDecimal contribution = b.weightSum.signum() <= 0 ? BigDecimal.ZERO
                    : line.rate.multiply(line.weight).divide(b.weightSum, 2, RoundingMode.HALF_UP);
            items.add(new GoalContributionVO.Item(line.projectId,
                    line.weight.setScale(2, RoundingMode.HALF_UP), line.rate, contribution));
        }
        return new GoalContributionVO(g.getId(), g.getTitle(),
                b.weightedRate.setScale(2, RoundingMode.HALF_UP), items);
    }

    /** 加权明细：遍历 KR 的全部项目对齐，聚合权重与加权完成率（权重≤0 跳过）。 */
    private Breakdown breakdown(Long goalId) {
        List<PmGoalAlignment> projectAligns = alignmentMapper.selectList(
                Wrappers.<PmGoalAlignment>lambdaQuery()
                        .eq(PmGoalAlignment::getGoalId, goalId)
                        .eq(PmGoalAlignment::getTargetType, "project"));
        List<Line> lines = new ArrayList<>();
        BigDecimal weightSum = BigDecimal.ZERO;
        BigDecimal rateWeighted = BigDecimal.ZERO;
        for (PmGoalAlignment a : projectAligns) {
            BigDecimal w = a.getWeight() == null ? BigDecimal.ONE : a.getWeight();
            if (w.signum() <= 0) {
                continue;
            }
            BigDecimal rate = completionPort.completionRate(a.getTargetId()); // 0–100
            if (rate == null) {
                rate = BigDecimal.ZERO;
            }
            lines.add(new Line(a.getTargetId(), w, rate));
            rateWeighted = rateWeighted.add(rate.multiply(w));
            weightSum = weightSum.add(w);
        }
        BigDecimal weightedRate = weightSum.signum() <= 0 ? BigDecimal.ZERO
                : rateWeighted.divide(weightSum, 4, RoundingMode.HALF_UP); // 0–100
        return new Breakdown(lines, weightSum, weightedRate);
    }

    private record Line(Long projectId, BigDecimal weight, BigDecimal rate) {
    }

    private record Breakdown(List<Line> lines, BigDecimal weightSum, BigDecimal weightedRate) {
    }

    private Map<String, Object> progressPayload(PmGoal g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("goalId", g.getId());
        m.put("progress", g.getProgress());
        m.put("source", "auto_rollup");
        return m;
    }
}
