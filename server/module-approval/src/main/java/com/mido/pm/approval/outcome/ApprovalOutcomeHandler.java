package com.mido.pm.approval.outcome;

/**
 * 审批结果处理器（port）：各业务域注册自己的 bizType 并实现回写。
 *
 * <p>事件解码 / payload guard / bizId 提取 / 异常兜底由 {@link ApprovalOutcomeRouter}
 * 统一承担；业务域只声明 bizType 与所需回调（驳回/撤回默认空实现，按需覆盖）。
 * 取代原各域重复的 {@code *ApprovalListener} 样板。
 */
public interface ApprovalOutcomeHandler {

    /** 本处理器认领的审批 bizType（对应 approval_instance.biz_type）。 */
    String bizType();

    /** 展示名（中文），作为前端 bizType 标签的单一信息源（经 /approvals/biz-types 暴露）。 */
    String label();

    /** 列表排序权重，小在前（用于 /approvals/biz-types 与前端下拉的稳定顺序）。 */
    default int order() {
        return 100;
    }

    /** 审批通过。 */
    void onApproved(long bizId);

    /** 审批驳回（默认无操作）。 */
    default void onRejected(long bizId) {
    }

    /** 发起人撤回（默认无操作）。 */
    default void onWithdrawn(long bizId) {
    }
}
