package com.mido.pm.approval.domain;

import java.util.List;

/**
 * 审批流节点。
 *
 * @param key       节点标识
 * @param name      节点名
 * @param approvers 审批人用户 ID 列表
 * @param mode      会签/或签：and 全部通过 / or 任一通过（默认 or）
 * @param guard     可插拔节点 guard 标识（如 JOB_LEVEL），为空则无 guard
 * @param cc        知会人用户 ID 列表
 * @param condition 路由条件（conditional 分支），为空则恒启用（fixed 固定流）
 */
public record FlowNode(
        String key,
        String name,
        List<Long> approvers,
        String mode,
        String guard,
        List<Long> cc,
        NodeCondition condition) {

    public static final String MODE_AND = "and";
    public static final String MODE_OR = "or";

    public boolean isCountersign() {
        return MODE_AND.equalsIgnoreCase(mode);
    }
}
