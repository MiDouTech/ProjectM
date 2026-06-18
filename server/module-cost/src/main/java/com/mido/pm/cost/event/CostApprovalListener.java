package com.mido.pm.cost.event;

import com.mido.pm.common.outbox.DomainEventMessage;
import com.mido.pm.cost.service.CostService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 监听 approval.approved / approval.rejected(biz_type=cost) → 回写费用状态（已发生 / 被退回）。
 * 复用审批引擎，费用域只订阅结果，不自造审批逻辑。AFTER_COMMIT：审批事务提交后才回写。
 */
@Component
public class CostApprovalListener {

    private final CostService costService;

    public CostApprovalListener(CostService costService) {
        this.costService = costService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEventMessage message) {
        boolean approved = "approval.approved".equals(message.eventType());
        boolean rejected = "approval.rejected".equals(message.eventType());
        if (!approved && !rejected) {
            return;
        }
        if (!(message.payload() instanceof Map<?, ?> payload)) {
            return;
        }
        if (!CostService.BIZ_TYPE.equals(payload.get("bizType"))) {
            return;
        }
        if (payload.get("bizId") instanceof Number num) {
            costService.applyApprovalResult(num.longValue(), approved);
        }
    }
}
