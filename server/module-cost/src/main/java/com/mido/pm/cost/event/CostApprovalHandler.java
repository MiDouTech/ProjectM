package com.mido.pm.cost.event;

import com.mido.pm.approval.outcome.ApprovalOutcomeHandler;
import com.mido.pm.cost.service.CostService;
import org.springframework.stereotype.Component;

/**
 * 费用审批结果回写：通过→已发生，驳回→被退回；撤回不处理（沿用原 CostApprovalListener 语义）。
 * 事件解码/guard/兜底由 {@link com.mido.pm.approval.outcome.ApprovalOutcomeRouter} 统一承担。
 */
@Component
public class CostApprovalHandler implements ApprovalOutcomeHandler {

    private final CostService costService;

    public CostApprovalHandler(CostService costService) {
        this.costService = costService;
    }

    @Override
    public String bizType() {
        return CostService.BIZ_TYPE;
    }

    @Override
    public void onApproved(long bizId) {
        costService.applyApprovalResult(bizId, true);
    }

    @Override
    public void onRejected(long bizId) {
        costService.applyApprovalResult(bizId, false);
    }
}
