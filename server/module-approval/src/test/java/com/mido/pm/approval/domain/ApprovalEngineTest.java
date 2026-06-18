package com.mido.pm.approval.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 审批引擎流转单测：或签/会签节点判定、驳回、条件分支活动节点解析。
 */
class ApprovalEngineTest {

    private FlowNode node(String key, List<Long> approvers, String mode, NodeCondition cond) {
        return new FlowNode(key, key, approvers, mode, null, List.of(), cond);
    }

    @Test
    void orNodePassesWhenAnyApproves() {
        FlowNode n = node("A", List.of(1L, 2L), FlowNode.MODE_OR, null);
        // 尚无通过、仍有待办 → 待定；任一通过 → 过（或签不要求待办清空）
        assertEquals(NodeStatus.PENDING, ApprovalEngine.evaluateNode(n, false, true, false));
        assertEquals(NodeStatus.PASSED, ApprovalEngine.evaluateNode(n, true, true, false));
    }

    @Test
    void andNodePassesOnlyWhenAllApprove() {
        FlowNode n = node("B", List.of(3L, 4L), FlowNode.MODE_AND, null);
        // 会签：已有通过但仍有待办 → 待定；全部处理完且有通过 → 过
        assertEquals(NodeStatus.PENDING, ApprovalEngine.evaluateNode(n, true, true, false));
        assertEquals(NodeStatus.PASSED, ApprovalEngine.evaluateNode(n, true, false, false));
    }

    @Test
    void rejectedShortCircuits() {
        FlowNode n = node("A", List.of(1L), FlowNode.MODE_OR, null);
        assertEquals(NodeStatus.REJECTED, ApprovalEngine.evaluateNode(n, true, false, true));
    }

    @Test
    void noTasksStaysPending() {
        FlowNode n = node("A", List.of(), FlowNode.MODE_OR, null);
        assertEquals(NodeStatus.PENDING, ApprovalEngine.evaluateNode(n, false, false, false));
    }

    @Test
    void conditionalRoutingByCategory() {
        FlowNode common = node("base", List.of(1L), FlowNode.MODE_OR, null);             // 恒启用
        FlowNode sOnly = node("vp", List.of(2L), FlowNode.MODE_OR,
                new NodeCondition("category", "==", "S"));                                // 仅 S 类启用
        FlowDefinition def = new FlowDefinition(List.of(common, sOnly));

        assertEquals(2, ApprovalEngine.activeNodes(def, new ApprovalContext(Map.of("category", "S"))).size());
        assertEquals(1, ApprovalEngine.activeNodes(def, new ApprovalContext(Map.of("category", "O"))).size());
    }

    @Test
    void conditionalRoutingSelectsActiveNodes() {
        FlowNode a = node("A", List.of(1L), FlowNode.MODE_OR, null);                       // 恒启用
        FlowNode b = node("B", List.of(2L), FlowNode.MODE_OR,
                new NodeCondition("amount", ">", "100000"));                                // 大额才启用
        FlowDefinition def = new FlowDefinition(List.of(a, b));

        assertEquals(2, ApprovalEngine.activeNodes(def, new ApprovalContext(Map.of("amount", 200000))).size());
        assertEquals(1, ApprovalEngine.activeNodes(def, new ApprovalContext(Map.of("amount", 50000))).size());
    }
}
