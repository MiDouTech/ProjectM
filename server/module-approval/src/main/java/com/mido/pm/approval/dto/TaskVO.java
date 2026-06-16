package com.mido.pm.approval.dto;

import java.time.LocalDateTime;

public record TaskVO(
        Long id,
        Long instanceId,
        String node,
        Long approverId,
        String action,
        String comment,
        LocalDateTime actedAt) {
}
