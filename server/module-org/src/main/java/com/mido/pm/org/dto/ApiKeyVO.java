package com.mido.pm.org.dto;

import java.time.LocalDateTime;

/** API Key 视图（不含明文，仅前缀）。 */
public record ApiKeyVO(
        Long id,
        String name,
        String keyPrefix,
        String status,
        Long userId,
        LocalDateTime lastUsedAt,
        LocalDateTime expireAt,
        LocalDateTime createTime) {
}
