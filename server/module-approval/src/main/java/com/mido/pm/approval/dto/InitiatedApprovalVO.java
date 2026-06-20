package com.mido.pm.approval.dto;

import java.time.LocalDateTime;

/**
 * 我发起的审批条目（审批中心「我发起的」列表）：当前用户提交的审批实例，跨 bizType、任意状态。
 * bizType/bizId 供前端定位业务对象；title 为业务展示标题（取自 formData，旧实例可能为空）。
 */
public record InitiatedApprovalVO(
        Long instanceId,
        String bizType,
        Long bizId,
        String status,
        String title,
        LocalDateTime submittedAt) {
}
