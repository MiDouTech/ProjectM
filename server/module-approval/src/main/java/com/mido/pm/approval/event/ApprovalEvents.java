package com.mido.pm.approval.event;

/** 审批域领域事件名（取自 docs/domain-events.md，禁自造）。 */
public final class ApprovalEvents {

    public static final String SUBMITTED = "approval.submitted";
    public static final String NODE_APPROVED = "approval.node.approved";
    public static final String APPROVED = "approval.approved";
    public static final String REJECTED = "approval.rejected";
    public static final String WITHDRAWN = "approval.withdrawn";
    public static final String TRANSFERRED = "approval.transferred";
    /** 节点审批人解析为空 → 自动跳过（告警，避免实例死锁待定） */
    public static final String NODE_SKIPPED = "approval.node.skipped";

    private ApprovalEvents() {
    }
}
