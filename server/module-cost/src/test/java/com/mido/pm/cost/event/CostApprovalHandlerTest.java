package com.mido.pm.cost.event;

import com.mido.pm.cost.service.CostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

/**
 * 费用审批结果 handler 单测：通过→已发生(applyApprovalResult true)，驳回→被退回(false)。
 * bizType 路由由 ApprovalOutcomeRouterTest 覆盖。
 */
@ExtendWith(MockitoExtension.class)
class CostApprovalHandlerTest {

    @Mock private CostService costService;
    @InjectMocks private CostApprovalHandler handler;

    @Test
    void bizTypeIsCost() {
        assertEquals(CostService.BIZ_TYPE, handler.bizType());
    }

    @Test
    void approvedAppliesOccurred() {
        handler.onApproved(5L);
        verify(costService).applyApprovalResult(5L, true);
    }

    @Test
    void rejectedAppliesReturned() {
        handler.onRejected(5L);
        verify(costService).applyApprovalResult(5L, false);
    }
}
