package com.mido.pm.change.dto;

import java.time.LocalDateTime;

/** 变更单视图（变更中心台账）。before/after 为 JSON 文本，前端按 changeType 渲染 diff。 */
public record ChangeRequestVO(
        Long id,
        String bizType,
        Long bizId,
        String changeType,
        String title,
        String reason,
        String impact,
        String beforeSnapshot,
        String afterPayload,
        String status,
        Long approvalInstanceId,
        LocalDateTime appliedAt,
        Long createBy,
        LocalDateTime createTime) {
}
