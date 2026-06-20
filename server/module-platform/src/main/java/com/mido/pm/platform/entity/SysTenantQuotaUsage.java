package com.mido.pm.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 租户用量快照（sys_tenant_quota_usage）。平台域全局表，tenant_id 为普通引用列。 */
@TableName("sys_tenant_quota_usage")
public class SysTenantQuotaUsage extends PlatformBaseEntity {

    private Long tenantId;
    private String resource;
    private Long usedValue;
    private LocalDateTime snapshotTime;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Long getUsedValue() {
        return usedValue;
    }

    public void setUsedValue(Long usedValue) {
        this.usedValue = usedValue;
    }

    public LocalDateTime getSnapshotTime() {
        return snapshotTime;
    }

    public void setSnapshotTime(LocalDateTime snapshotTime) {
        this.snapshotTime = snapshotTime;
    }
}
