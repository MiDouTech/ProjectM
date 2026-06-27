package com.mido.pm.platform.dto;

/** 跨租户用量监控查询。onlyExceeded=true 仅返回存在超限资源的租户。 */
public record UsageMonitorQueryDTO(Long page, Long size, Boolean onlyExceeded) {
}
