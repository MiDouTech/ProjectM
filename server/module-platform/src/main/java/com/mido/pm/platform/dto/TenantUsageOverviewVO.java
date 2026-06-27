package com.mido.pm.platform.dto;

import java.util.List;

/**
 * 跨租户用量监控行：单租户 + 其各资源用量/上限/是否超限。
 *
 * @param tenantId    租户 ID
 * @param tenantCode  租户编码
 * @param tenantName  租户名称
 * @param status      租户状态
 * @param usage       各资源用量视图
 * @param anyExceeded 是否存在任一资源超限
 */
public record TenantUsageOverviewVO(Long tenantId, String tenantCode, String tenantName, String status,
                                    List<TenantUsageVO> usage, boolean anyExceeded) {
}
