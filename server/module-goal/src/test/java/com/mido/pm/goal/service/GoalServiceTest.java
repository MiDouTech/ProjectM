package com.mido.pm.goal.service;

import com.mido.pm.change.service.ChangeService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.goal.entity.PmGoal;
import com.mido.pm.goal.event.GoalEvents;
import com.mido.pm.goal.mapper.PmGoalAlignmentMapper;
import com.mido.pm.goal.mapper.PmGoalMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 弱关联红线单测：删目标只删其对齐链、不级联删对方；对方(task)删除时只清对齐链、不动目标。
 */
@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock private PmGoalMapper goalMapper;
    @Mock private PmGoalAlignmentMapper alignmentMapper;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private GoalRollupService rollupService;
    @Mock private ChangeService changeService;
    @InjectMocks private GoalService service;

    @Test
    void updateBlockedWhenChangePending() {
        PmGoal g = new PmGoal();
        g.setId(5L);
        when(goalMapper.selectById(5L)).thenReturn(g);
        // 受控变更冻结：有进行中变更单时，直接编辑基线必须被拒（防绕过审批）
        when(changeService.hasPending(GoalChangeService.BIZ_TYPE, 5L)).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertThrows(BizException.class, () -> service.update(5L, null));
        verify(goalMapper, never()).updateById(any(PmGoal.class));
    }

    @Test
    void deleteBlockedWhenChangePending() {
        PmGoal g = new PmGoal();
        g.setId(5L);
        when(goalMapper.selectById(5L)).thenReturn(g);
        when(changeService.hasPending(GoalChangeService.BIZ_TYPE, 5L)).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertThrows(BizException.class, () -> service.delete(5L));
        verify(goalMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteGoalRemovesOwnAlignmentsOnly() {
        PmGoal g = new PmGoal();
        g.setId(5L);
        when(goalMapper.selectById(5L)).thenReturn(g);

        service.delete(5L);

        // 删目标本体 + 删其对齐链；GoalService 无 project/task mapper，结构上不可能级联删对方
        verify(goalMapper).deleteById(5L);
        verify(alignmentMapper).delete(any());
    }

    @Test
    void targetDeletionRemovesAlignmentLinksNotGoal() {
        when(alignmentMapper.delete(any())).thenReturn(2);
        service.removeAlignmentsByTarget("task", 9L);

        // 只删对齐链；从不触碰 goalMapper（不级联删目标）；删到行才发 unaligned 事件
        verify(alignmentMapper).delete(any());
        verify(goalMapper, never()).deleteById(eq(9L));
        verify(eventPublisher).publish(eq(GoalEvents.UNALIGNED), any());
    }

    @Test
    void targetDeletionWithNoLinksEmitsNoEvent() {
        when(alignmentMapper.delete(any())).thenReturn(0);
        service.removeAlignmentsByTarget("project", 77L);

        verify(eventPublisher, never()).publish(eq(GoalEvents.UNALIGNED), any());
    }

    @Test
    void listGoalsByTargetMapsAlignmentId() {
        com.mido.pm.goal.entity.PmGoalAlignment a = new com.mido.pm.goal.entity.PmGoalAlignment();
        a.setId(100L);
        a.setGoalId(7L);
        a.setTargetType("project");
        a.setTargetId(42L);
        when(alignmentMapper.selectList(any())).thenReturn(java.util.List.of(a));
        PmGoal g = new PmGoal();
        g.setId(7L);
        g.setTitle("KR-A");
        when(goalMapper.selectBatchIds(any())).thenReturn(java.util.List.of(g));

        var result = service.listGoalsByTarget("project", 42L);

        org.junit.jupiter.api.Assertions.assertEquals(1, result.size());
        org.junit.jupiter.api.Assertions.assertEquals(100L, result.get(0).alignmentId());
        org.junit.jupiter.api.Assertions.assertEquals(7L, result.get(0).goal().id());
    }

    @Test
    void listGoalsByTargetRejectsBadType() {
        org.junit.jupiter.api.Assertions.assertThrows(
                com.mido.pm.common.exception.BizException.class,
                () -> service.listGoalsByTarget("user", 1L));
    }
}
