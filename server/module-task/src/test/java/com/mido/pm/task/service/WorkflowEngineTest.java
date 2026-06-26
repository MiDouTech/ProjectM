package com.mido.pm.task.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.task.entity.PmStatus;
import com.mido.pm.task.entity.PmWorkItemType;
import com.mido.pm.task.mapper.PmStatusMapper;
import com.mido.pm.task.mapper.PmWorkItemTypeMapper;
import com.mido.pm.task.mapper.PmWorkflowTransitionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 工作流引擎单测：已配置租户走矩阵；未配置租户回落 TaskWorkflow。
 */
@ExtendWith(MockitoExtension.class)
class WorkflowEngineTest {

    @Mock private PmWorkItemTypeMapper typeMapper;
    @Mock private PmStatusMapper statusMapper;
    @Mock private PmWorkflowTransitionMapper transitionMapper;

    private WorkflowEngine engine() {
        return new WorkflowEngine(typeMapper, statusMapper, transitionMapper);
    }

    private PmWorkItemType type() {
        PmWorkItemType t = new PmWorkItemType();
        t.setId(1L);
        t.setCode("task");
        return t;
    }

    private PmStatus status(long id) {
        PmStatus s = new PmStatus();
        s.setId(id);
        return s;
    }

    @Test
    void matrixAllowsConfiguredTransition() {
        when(typeMapper.selectList(any())).thenReturn(List.of(type()));
        when(statusMapper.selectList(any())).thenReturn(List.of(status(2L)));
        when(transitionMapper.selectCount(any())).thenReturn(1L);
        assertDoesNotThrow(() -> engine().assertTaskTransit("进行中", "已完成"));
    }

    @Test
    void matrixRejectsUnconfiguredTransition() {
        when(typeMapper.selectList(any())).thenReturn(List.of(type()));
        when(statusMapper.selectList(any())).thenReturn(List.of(status(2L)));
        when(transitionMapper.selectCount(any())).thenReturn(0L);
        assertThrows(BizException.class, () -> engine().assertTaskTransit("进行中", "已完成"));
    }

    @Test
    void fallsBackToTaskWorkflowWhenNoTypeConfigured() {
        // 未配置默认类型 → 回落 TaskWorkflow：未开始→进行中合法，未开始→已完成非法
        lenient().when(typeMapper.selectList(any())).thenReturn(List.of());
        assertDoesNotThrow(() -> engine().assertTaskTransit("未开始", "进行中"));
        assertThrows(BizException.class, () -> engine().assertTaskTransit("未开始", "已完成"));
    }
}
