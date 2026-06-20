package com.mido.pm.project.event;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.project.dto.ProjectTransitionDTO;
import com.mido.pm.project.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * 立项审批结果 handler 单测：通过→已注册(approvalPassed)；驳回/撤回→草稿；
 * 状态机以 BizException 拒绝时良性吞掉（不外抛）。bizType 路由由 ApprovalOutcomeRouterTest 覆盖。
 */
@ExtendWith(MockitoExtension.class)
class ProjectApprovalHandlerTest {

    @Mock private ProjectService projectService;
    @InjectMocks private ProjectApprovalHandler handler;

    @Test
    void approvedTriggersRegister() {
        handler.onApproved(42L);
        verify(projectService).transition(eq(42L), argThat((ProjectTransitionDTO d) ->
                "已注册".equals(d.targetStatus()) && Boolean.TRUE.equals(d.approvalPassed())));
    }

    @Test
    void rejectedTriggersDraft() {
        handler.onRejected(42L);
        verify(projectService).transition(eq(42L), argThat((ProjectTransitionDTO d) ->
                "草稿".equals(d.targetStatus())));
    }

    @Test
    void withdrawnTriggersDraft() {
        handler.onWithdrawn(42L);
        verify(projectService).transition(eq(42L), argThat((ProjectTransitionDTO d) ->
                "草稿".equals(d.targetStatus())));
    }

    @Test
    void benignBizExceptionSwallowed() {
        doThrow(new BizException(ErrorCode.CONFLICT, "状态已变更"))
                .when(projectService).transition(eq(42L), any());
        assertDoesNotThrow(() -> handler.onApproved(42L));
    }
}
