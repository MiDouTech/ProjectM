package com.mido.pm.change.domain;

/** 变更单状态常量。 */
public final class ChangeStatus {

    public static final String DRAFT = "draft";
    public static final String PENDING = "pending";       // 已提交，审批中
    public static final String APPROVED = "approved";     // 审批通过，待应用（自动应用模式下瞬态）
    public static final String APPLIED = "applied";       // 已回写被改实体
    public static final String REJECTED = "rejected";
    public static final String WITHDRAWN = "withdrawn";

    private ChangeStatus() {
    }
}
