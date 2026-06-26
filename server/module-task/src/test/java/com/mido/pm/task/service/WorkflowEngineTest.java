package com.mido.pm.task.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.task.mapper.PmWorkItemTransitionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 工作流引擎单测：已配置租户走矩阵；未配置租户回落 TaskWorkflow。id 解析由 resolver 提供。
 */
@ExtendWith(MockitoExtension.class)
class WorkflowEngineTest {

    @Mock private WorkItemMetaResolver resolver;
    @Mock private PmWorkItemTransitionMapper transitionMapper;

    private WorkflowEngine engine() {
        return new WorkflowEngine(resolver, transitionMapper);
    }

    @Test
    void matrixAllowsConfiguredTransition() {
        when(resolver.defaultTaskTypeId()).thenReturn(1L);
        when(resolver.statusIdByName(any())).thenReturn(2L);
        when(transitionMapper.selectCount(any())).thenReturn(1L);
        assertDoesNotThrow(() -> engine().assertTaskTransit("进行中", "已完成"));
    }

    @Test
    void matrixRejectsUnconfiguredTransition() {
        when(resolver.defaultTaskTypeId()).thenReturn(1L);
        when(resolver.statusIdByName(any())).thenReturn(2L);
        when(transitionMapper.selectCount(any())).thenReturn(0L);
        assertThrows(BizException.class, () -> engine().assertTaskTransit("进行中", "已完成"));
    }

    @Test
    void fallsBackToTaskWorkflowWhenNoTypeConfigured() {
        // 未配置默认类型 → 回落 TaskWorkflow：未开始→进行中合法，未开始→已完成非法
        lenient().when(resolver.defaultTaskTypeId()).thenReturn(null);
        assertDoesNotThrow(() -> engine().assertTaskTransit("未开始", "进行中"));
        assertThrows(BizException.class, () -> engine().assertTaskTransit("未开始", "已完成"));
    }
}
