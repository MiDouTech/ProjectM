package com.mido.pm.goal.service;

import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.goal.domain.ProjectCompletionPort;
import com.mido.pm.goal.entity.PmGoal;
import com.mido.pm.goal.entity.PmGoalAlignment;
import com.mido.pm.goal.event.GoalEvents;
import com.mido.pm.goal.mapper.PmGoalAlignmentMapper;
import com.mido.pm.goal.mapper.PmGoalMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 目标进度自动汇总单测：多项目按对齐权重加权 → 映射到 KR 量纲反写；非 auto_rollup 的 KR 不动。
 */
@ExtendWith(MockitoExtension.class)
class GoalRollupServiceTest {

    @Mock private PmGoalMapper goalMapper;
    @Mock private PmGoalAlignmentMapper alignmentMapper;
    @Mock private ProjectCompletionPort completionPort;
    @Mock private DomainEventPublisher eventPublisher;

    private GoalRollupService service() {
        return new GoalRollupService(goalMapper, alignmentMapper, completionPort, eventPublisher);
    }

    private PmGoalAlignment align(long goalId, long targetId, double weight) {
        PmGoalAlignment a = new PmGoalAlignment();
        a.setGoalId(goalId);
        a.setTargetType("project");
        a.setTargetId(targetId);
        a.setWeight(BigDecimal.valueOf(weight));
        return a;
    }

    private PmGoal kr(int autoRollup) {
        PmGoal g = new PmGoal();
        g.setId(7L);
        g.setType("kr");
        g.setMetricStart(BigDecimal.ZERO);
        g.setMetricTarget(BigDecimal.valueOf(100));
        g.setAutoRollup(autoRollup);
        return g;
    }

    @Test
    void weightedRollupWritesMappedCurrentAndProgress() {
        // KR-7 对齐项目 42(权重1,完成率80) 与 50(权重3,完成率40) → 加权完成率=(80*1+40*3)/4=50
        List<PmGoalAlignment> aligns = List.of(align(7L, 42L, 1), align(7L, 50L, 3));
        when(alignmentMapper.selectList(any())).thenReturn(aligns);
        when(goalMapper.selectById(7L)).thenReturn(kr(1));
        when(completionPort.completionRate(42L)).thenReturn(BigDecimal.valueOf(80));
        when(completionPort.completionRate(50L)).thenReturn(BigDecimal.valueOf(40));

        service().recomputeForProject(42L);

        ArgumentCaptor<PmGoal> cap = ArgumentCaptor.forClass(PmGoal.class);
        verify(goalMapper).updateById(cap.capture());
        // current = 0 + 50% × (100-0) = 50；progress = 50
        assertEquals(0, cap.getValue().getMetricCurrent().compareTo(BigDecimal.valueOf(50)));
        assertEquals(0, cap.getValue().getProgress().compareTo(BigDecimal.valueOf(50)));
        verify(eventPublisher).publish(eq(GoalEvents.PROGRESS_CHANGED), any());
    }

    @Test
    void contributionBreaksDownPerProject() {
        // 各项贡献 = 完成率×权重/Σ权重；项目42=80×1/4=20，项目50=40×3/4=30，合计=加权完成率50
        List<PmGoalAlignment> aligns = List.of(align(7L, 42L, 1), align(7L, 50L, 3));
        when(alignmentMapper.selectList(any())).thenReturn(aligns);
        when(goalMapper.selectById(7L)).thenReturn(kr(1));
        when(completionPort.completionRate(42L)).thenReturn(BigDecimal.valueOf(80));
        when(completionPort.completionRate(50L)).thenReturn(BigDecimal.valueOf(40));

        var vo = service().contribution(7L);

        assertEquals(0, vo.weightedRate().compareTo(BigDecimal.valueOf(50)));
        assertEquals(2, vo.items().size());
        assertEquals(0, vo.items().get(0).contribution().compareTo(BigDecimal.valueOf(20)));
        assertEquals(0, vo.items().get(1).contribution().compareTo(BigDecimal.valueOf(30)));
    }

    @Test
    void nonAutoRollupKrUntouched() {
        when(alignmentMapper.selectList(any())).thenReturn(List.of(align(7L, 42L, 1)));
        when(goalMapper.selectById(7L)).thenReturn(kr(0)); // auto_rollup=0
        service().recomputeForProject(42L);
        verify(goalMapper, never()).updateById(any(PmGoal.class));
        verify(eventPublisher, never()).publish(any(), any());
    }
}
