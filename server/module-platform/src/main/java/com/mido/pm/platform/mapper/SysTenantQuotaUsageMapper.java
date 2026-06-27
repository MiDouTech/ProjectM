package com.mido.pm.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mido.pm.platform.entity.SysTenantQuotaUsage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/** SysTenantQuotaUsage Mapper。 */
@Mapper
public interface SysTenantQuotaUsageMapper extends BaseMapper<SysTenantQuotaUsage> {

    /**
     * 原子 upsert 用量快照：依赖唯一键 uk_usage_tenant_res(tenant_id, resource)，
     * 存在则更新用量与快照时间，否则插入。避免"先查后写"的竞态与 N+1。
     */
    @Insert("INSERT INTO sys_tenant_quota_usage (id, tenant_id, resource, used_value, snapshot_time) "
            + "VALUES (#{id}, #{tenantId}, #{resource}, #{usedValue}, #{snapshotTime}) "
            + "ON DUPLICATE KEY UPDATE used_value = VALUES(used_value), snapshot_time = VALUES(snapshot_time)")
    int upsert(SysTenantQuotaUsage row);
}
