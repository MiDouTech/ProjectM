package com.mido.pm.approval.domain;

import java.util.List;
import java.util.Set;

/**
 * 审批引擎核心（纯逻辑、可单测）：解析活动节点 + 单节点通过判定（会签/或签/驳回）。
 * 持久化与事件由服务层编排，决策委托本引擎。
 */
public final class ApprovalEngine {

    private ApprovalEngine() {
    }

    /** 按条件路由解析本实例的活动节点（有序）。 */
    public static List<FlowNode> activeNodes(FlowDefinition definition, ApprovalContext ctx) {
        return FlowResolver.resolveActiveNodes(definition, ctx);
    }

    /**
     * 判定某节点状态。
     *
     * @param node             节点
     * @param approvedApprovers 已点通过的审批人集合
     * @param rejected         是否已有人驳回
     */
    public static NodeStatus evaluateNode(FlowNode node, Set<Long> approvedApprovers, boolean rejected) {
        if (rejected) {
            return NodeStatus.REJECTED;
        }
        List<Long> approvers = node.approvers() == null ? List.of() : node.approvers();
        if (approvers.isEmpty()) {
            return NodeStatus.PENDING;
        }
        if (node.isCountersign()) {
            // 会签：全部通过
            return approvedApprovers.containsAll(approvers) ? NodeStatus.PASSED : NodeStatus.PENDING;
        }
        // 或签：任一通过
        return approvers.stream().anyMatch(approvedApprovers::contains)
                ? NodeStatus.PASSED : NodeStatus.PENDING;
    }
}
