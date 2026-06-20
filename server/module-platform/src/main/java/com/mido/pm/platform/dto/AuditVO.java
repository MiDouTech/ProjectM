package com.mido.pm.platform.dto;

import java.time.LocalDateTime;

/** 运营审计视图。 */
public record AuditVO(
        Long id,
        Long adminId,
        String adminName,
        String action,
        String target,
        Long targetId,
        Object detail,
        String ip,
        LocalDateTime createTime) {
}
