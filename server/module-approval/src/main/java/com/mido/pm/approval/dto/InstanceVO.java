package com.mido.pm.approval.dto;

import java.util.List;

/**
 * 审批实例视图。currentNodeName/currentMode/pendingApproverIds/approvedApproverIds
 * 用于前端展示「待谁审批」（当前节点名、会签 and/或签 or、待办与已通过审批人）。
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
        List<Long> approvedApproverIds) {
}
