package com.mido.pm.approval.domain;

import java.util.List;

/**
 * 流程节点解析（纯函数）：按节点条件过滤出本实例启用的有序节点（fixed 全启用 / conditional 按条件）。
 */
public final class FlowResolver {

    private FlowResolver() {
    }

    public static List<FlowNode> resolveActiveNodes(FlowDefinition definition, ApprovalContext ctx) {
        if (definition == null || definition.nodes() == null) {
            return List.of();
        }
        return definition.nodes().stream()
                .filter(n -> ConditionEvaluator.evaluate(n.condition(), ctx))
                .toList();
    }
}
