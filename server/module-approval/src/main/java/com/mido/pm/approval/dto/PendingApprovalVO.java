package com.mido.pm.approval.dto;

import java.time.LocalDateTime;

/**
 * 待我审批条目（工作台卡）：当前用户在某 pending 实例上的未处理待办。
 * bizType/bizId 供前端定位业务对象（如立项 project_init → 项目）。
 */
public record PendingApprovalVO(
        Long instanceId,
        String bizType,
        Long bizId,
        String node,
        Long applicantId,
        LocalDateTime submittedAt) {
}
