package com.mido.pm.platform.dto;

import java.util.List;
import java.util.Map;

/**
 * 运营概览。
 *
 * @param totalTenants   租户总数（含各状态）
 * @param activeTenants  正式状态租户数
 * @param trialTenants   试用状态租户数
 * @param newThisMonth   本月新增租户数
 * @param statusDist     状态分布（status → 数量）
 * @param expiringSoon   30 天内到期租户（按到期时间升序）
 */
public record DashboardVO(
        long totalTenants,
        long activeTenants,
        long trialTenants,
        long newThisMonth,
        Map<String, Long> statusDist,
        List<TenantVO> expiringSoon) {
}
