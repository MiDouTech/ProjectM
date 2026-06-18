package com.mido.pm.goal.service;

import com.mido.pm.goal.entity.PmGoal;
import com.mido.pm.goal.mapper.PmGoalAlignmentMapper;
import com.mido.pm.goal.mapper.PmGoalMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 弱关联红线单测：删目标只删其对齐链、不级联删对方；对方(task)删除时只清对齐链、不动目标。
 */
@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock private PmGoalMapper goalMapper;
    @Mock private PmGoalAlignmentMapper alignmentMapper;
    @InjectMocks private GoalService service;

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
        service.removeAlignmentsByTarget("task", 9L);

        // 只删对齐链；从不触碰 goalMapper（不级联删目标）
        verify(alignmentMapper).delete(any());
        verify(goalMapper, org.mockito.Mockito.never()).deleteById(eq(9L));
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
