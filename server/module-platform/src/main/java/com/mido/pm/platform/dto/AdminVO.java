package com.mido.pm.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 平台运营账号视图。 */
public record AdminVO(
        Long id,
        String username,
        String name,
        String status,
        List<Long> roleIds,
        List<String> roleNames,
        LocalDateTime lastLoginAt,
        LocalDateTime createTime) {
}
