package com.mido.pm.platform.dto;

/** 线下收入台账分页查询。 */
public record RevenueQueryDTO(Long tenantId, String type, Long page, Long size) {
}
