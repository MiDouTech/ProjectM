package com.mido.pm.platform.dto;

/** 租户分页查询：keyword 匹配名称/编码；status 过滤；分页参数。 */
public record TenantQueryDTO(String keyword, String status, Long page, Long size) {
}
