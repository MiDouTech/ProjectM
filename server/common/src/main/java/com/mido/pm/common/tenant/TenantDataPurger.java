package com.mido.pm.common.tenant;

/**
 * 租户数据清除端口：注销宽限期满后，各业务域物理清除指定租户的数据（合规）。
 * 实现按 tenant_id 物理删除自有表（忽略多租户拦截器、显式 tenant 过滤）。平台清除任务编排调用。
 */
public interface TenantDataPurger {

    /** 清除域名（用于日志/审计）。 */
    String domain();

    /**
     * 物理清除指定租户在本域的数据。
     *
     * @param tenantId 目标租户 ID
     * @return 删除的行数（用于汇总）
     */
    long purge(Long tenantId);
}
