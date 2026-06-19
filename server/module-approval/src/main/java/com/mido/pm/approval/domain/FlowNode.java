package com.mido.pm.approval.domain;

import java.util.List;

/**
 * 审批流节点。
 *
 * @param key            节点标识
 * @param name           节点名
 * @param approvers      指定成员审批人用户 ID 列表（approverType=USER 或未指定时生效）
 * @param mode           会签/或签：and 全部通过 / or 任一通过（默认 or）
 * @param guard          可插拔节点 guard 标识（如 JOB_LEVEL），为空则无 guard
 * @param cc             知会人用户 ID 列表
 * @param condition      路由条件（conditional 分支），为空则恒启用（fixed 固定流）
 * @param approverType   审批人类型（USER/ROLE/DEPT_HEAD/DIRECT_LEADER/APPLICANT_SELF），为空按 USER
 * @param approverValues 审批人类型参数（ROLE→角色ID列表 / DEPT_HEAD→[向上层级数]），USER 不用
 */
public record FlowNode(
        String key,
        String name,
        List<Long> approvers,
        String mode,
        String guard,
        List<Long> cc,
        NodeCondition condition,
        String approverType,
        List<Long> approverValues) {

    public static final String MODE_AND = "and";
    public static final String MODE_OR = "or";

    /** 兼容旧 7 参构造（无动态审批人字段，按 USER 型，审批人取 approvers）。 */
    public FlowNode(String key, String name, List<Long> approvers, String mode, String guard,
                    List<Long> cc, NodeCondition condition) {
        this(key, name, approvers, mode, guard, cc, condition, null, null);
    }

    public boolean isCountersign() {
        return MODE_AND.equalsIgnoreCase(mode);
    }

    /** 解析后的审批人类型（null/空→USER）。 */
    public ApproverType resolvedApproverType() {
        return ApproverType.from(approverType);
    }
}
