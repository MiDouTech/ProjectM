package com.mido.pm.platform.dto;

import java.time.LocalDateTime;

/** 租户列表项。planName 为当前生效订阅的套餐名（无则空）。 */
public record TenantVO(
        Long id,
        String code,
        String name,
        String status,
        String industry,
        String contactName,
        String contactPhone,
        String planName,
        LocalDateTime expireAt,
        LocalDateTime createTime) {
}
