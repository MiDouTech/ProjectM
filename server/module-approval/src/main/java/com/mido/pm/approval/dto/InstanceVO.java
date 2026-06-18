package com.mido.pm.approval.dto;

import java.util.List;
import java.util.Map;

/**
 * 审批实例视图。currentNodeName/currentMode/pendingApproverIds/approvedApproverIds
 * 用于前端展示「待谁审批」（当前节点名、会签 and/或签 or、待办与已通过审批人）。
 * formData 为业务提交快照（如立项的目标/预算/项目名），供审批页展示业务上下文。
 */
public record InstanceVO(
        Long id,
        Long flowId,
        String bizType,
        Long bizId,
        String status,
        String currentNode,
        Long applicantId,
        String currentNodeName,
        String currentMode,
        List<Long> pendingApproverIds,
        List<Long> approvedApproverIds,
        Map<String, Object> formData) {
}
