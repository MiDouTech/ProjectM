package com.mido.pm.approval.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.approval.dto.FlowCreateDTO;
import com.mido.pm.approval.entity.ApprovalFlow;
import com.mido.pm.approval.mapper.ApprovalFlowMapper;
import com.mido.pm.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审批流定义管理单测：definition JSON 校验 + 创建/更新（可视化设计器后端）。
 */
@ExtendWith(MockitoExtension.class)
class ApprovalFlowServiceTest {

    private static final String VALID_DEF =
            "{\"nodes\":[{\"key\":\"n1\",\"name\":\"部门负责人\",\"approvers\":[10],\"mode\":\"or\"}]}";

    @Mock private ApprovalFlowMapper flowMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ApprovalFlowService service() {
        return new ApprovalFlowService(flowMapper, objectMapper);
    }

    @Test
    void createRejectsInvalidDefinitionJson() {
        assertThrows(BizException.class,
                () -> service().create(new FlowCreateDTO("流A", "流程A", "project_init", "or", "{not-json")));
        verify(flowMapper, never()).insert(any(ApprovalFlow.class));
    }

    @Test
    void createPersistsValidFlow() {
        service().create(new FlowCreateDTO("流A", "流程A", "project_init", "or", VALID_DEF));
        verify(flowMapper).insert(any(ApprovalFlow.class));
    }

    @Test
    void updateRejectsWhenFlowMissing() {
        when(flowMapper.selectById(9L)).thenReturn(null);
        assertThrows(BizException.class,
                () -> service().update(9L, new FlowCreateDTO("流A", "流程A", "project_init", "or", VALID_DEF)));
        verify(flowMapper, never()).updateById(any(ApprovalFlow.class));
    }

    @Test
    void updatePersistsValidDefinition() {
        when(flowMapper.selectById(1L)).thenReturn(new ApprovalFlow());
        service().update(1L, new FlowCreateDTO("流B", "流程B", "project_init", "and", VALID_DEF));
        verify(flowMapper).updateById(any(ApprovalFlow.class));
    }
}
