package com.mido.pm.change.event;

import com.mido.pm.change.service.ChangeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * 变更审批结果 handler 单测：通过→应用变更；驳回→onApprovalClosed(true)；撤回→onApprovalClosed(false)。
 * bizType 路由由 ApprovalOutcomeRouterTest 覆盖。
 */
@ExtendWith(MockitoExtension.class)
class ChangeApprovalHandlerTest {

    @Mock private ChangeService changeService;
    @InjectMocks private ChangeApprovalHandler handler;

    @Test
    void approvedAppliesChange() {
        handler.onApproved(7L);
        verify(changeService).onApprovalApproved(7L);
    }

    @Test
    void rejectedClosesAsRejected() {
        handler.onRejected(7L);
        verify(changeService).onApprovalClosed(7L, true);
    }

    @Test
    void withdrawnClosesAsWithdrawn() {
        handler.onWithdrawn(7L);
        verify(changeService).onApprovalClosed(7L, false);
    }
}
