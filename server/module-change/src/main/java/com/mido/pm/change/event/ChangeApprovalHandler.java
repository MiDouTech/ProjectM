package com.mido.pm.change.event;

import com.mido.pm.approval.outcome.ApprovalOutcomeHandler;
import com.mido.pm.change.service.ChangeService;
import org.springframework.stereotype.Component;

/**
 * 变更审批结果回写：通过→应用变更（回写被改实体）；驳回→置驳回；撤回→置撤回（实体不动）。
 * 异常由 {@link com.mido.pm.approval.outcome.ApprovalOutcomeRouter} 兜底（变更单可能滞留 pending，需人工介入）。
 */
@Component
public class ChangeApprovalHandler implements ApprovalOutcomeHandler {

    private final ChangeService changeService;

    public ChangeApprovalHandler(ChangeService changeService) {
        this.changeService = changeService;
    }

    @Override
    public String bizType() {
        return ChangeService.APPROVAL_BIZ_TYPE;
    }

    @Override
    public String label() {
        return "变更审批";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public void onApproved(long bizId) {
        changeService.onApprovalApproved(bizId);
    }

    @Override
    public void onRejected(long bizId) {
        changeService.onApprovalClosed(bizId, true);
    }

    @Override
    public void onWithdrawn(long bizId) {
        changeService.onApprovalClosed(bizId, false);
    }
}
