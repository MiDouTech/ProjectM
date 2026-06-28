package com.mido.pm.approval.dto;

import java.time.LocalDateTime;

/**
 * 与我相关的审批条目（审批中心「全部」列表）：当前用户发起的、待我处理的、或我已处理过的审批实例，
 * 跨 bizType、任意状态，按实例新→旧。bizType/bizId 供前端定位业务对象；title 取自 formData（旧实例可能为空）。
 * 三个角色标记供前端做「全部/待我审批/我发起的/我已处理」筛选，避免再发多次请求：
 * mineToAct=当前待我处理（实例 pending 且我有未办待办）；iInitiated=我发起；processedByMe=我已处理过。
 */
public record RelatedApprovalVO(
        Long instanceId,
        String bizType,
        Long bizId,
        String status,
        Long applicantId,
        String title,
        LocalDateTime submittedAt,
        boolean mineToAct,
        boolean iInitiated,
        boolean processedByMe) {
}
