package com.mido.pm.cost.event;

import com.mido.pm.common.outbox.DomainEventMessage;
import com.mido.pm.cost.service.CostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 费用审批结果监听器单测：approval.approved/rejected(biz_type=cost) → 回写费用；其余忽略。
 * 驳回路径此前因 REJECTED 事件不带 bizType 而沉睡，现 payload 已补 bizType/bizId，必须覆盖。
 */
@ExtendWith(MockitoExtension.class)
class CostApprovalListenerTest {

    @Mock private CostService costService;
    @InjectMocks private CostApprovalListener listener;

    @Test
    void rejectedCostAppliesReturned() {
        listener.onDomainEvent(new DomainEventMessage("approval.rejected",
                Map.of("bizType", CostService.BIZ_TYPE, "bizId", 5L), 1L));

        verify(costService).applyApprovalResult(5L, false);
    }

    @Test
    void approvedCostAppliesOccurred() {
        listener.onDomainEvent(new DomainEventMessage("approval.approved",
                Map.of("bizType", CostService.BIZ_TYPE, "bizId", 5L), 1L));

        verify(costService).applyApprovalResult(5L, true);
    }

    @Test
    void otherBizTypeIgnored() {
        listener.onDomainEvent(new DomainEventMessage("approval.rejected",
                Map.of("bizType", "project_init", "bizId", 5L), 1L));

        verifyNoInteractions(costService);
    }
}
