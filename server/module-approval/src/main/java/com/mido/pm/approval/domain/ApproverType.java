package com.mido.pm.approval.domain;

/**
 * 审批人类型（行业通用）：决定节点审批人如何解析为具体用户。
 * 兼容旧定义：未指定（null/空）按 {@link #USER} 处理，审批人取节点 approvers 列表。
 */
public enum ApproverType {

    /** 指定成员：approvers 即用户 ID 列表 */
    USER,
    /** 角色：approverValues 为角色 ID，解析为拥有该角色的全部成员 */
    ROLE,
    /** 部门主管（逐级）：approverValues 首值为向上层级数（默认 1），解析发起人对应层级部门负责人 */
    DEPT_HEAD,
    /** 发起人直属上级：发起人所在部门负责人（等价 DEPT_HEAD 层级 1） */
    DIRECT_LEADER,
    /** 发起人本人 */
    APPLICANT_SELF;

    public static ApproverType from(String s) {
        if (s == null || s.isBlank()) {
            return USER;
        }
        try {
            return valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }
}
