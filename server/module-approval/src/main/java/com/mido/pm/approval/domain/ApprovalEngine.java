package com.mido.pm.approval.domain;

import java.util.List;

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
     * 判定某节点状态。基于该节点的动态待办任务状态（而非静态审批人列表），
     * 以支持「转交」——转交产生新待办，受让人通过即可推进，不依赖其是否在原始审批人名单。
     *
     * @param node        节点（仅取会签/或签模式）
     * @param anyApproved 该节点是否已有审批人点了通过
     * @param anyPending  该节点是否仍有未处理待办（含转交产生的新待办）
     * @param rejected    是否已有人驳回
     */
    public static NodeStatus evaluateNode(FlowNode node, boolean anyApproved, boolean anyPending, boolean rejected) {
        if (rejected) {
            return NodeStatus.REJECTED;
        }
        if (!anyApproved && !anyPending) {
            // 节点无任何待办（未配置审批人）：保持待定，避免空节点误判通过
            return NodeStatus.PENDING;
        }
        if (node.isCountersign()) {
            // 会签：全部待办处理完且至少一人通过
            return (anyApproved && !anyPending) ? NodeStatus.PASSED : NodeStatus.PENDING;
        }
        // 或签：任一通过即过
        return anyApproved ? NodeStatus.PASSED : NodeStatus.PENDING;
    }
}
