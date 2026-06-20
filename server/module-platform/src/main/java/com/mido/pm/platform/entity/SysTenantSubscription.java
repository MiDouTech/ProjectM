package com.mido.pm.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 租户订阅（sys_tenant_subscription）。平台域全局表。
 * 注意：此处 {@code tenantId} 是指向 sys_tenant.id 的<strong>普通引用列</strong>（非隔离列），
 * 本表在多租户拦截器忽略名单中，不会被自动注入条件；插入时必须显式赋值。
 * status：active/expired/cancelled。
 */
@TableName("sys_tenant_subscription")
public class SysTenantSubscription extends PlatformBaseEntity {

    /** 所属租户（sys_tenant.id）；普通引用列，需显式赋值 */
    private Long tenantId;
    private Long planId;
    private LocalDateTime startAt;
    private LocalDateTime expireAt;
    /** active/expired/cancelled */
    private String status;
    /** 配额覆盖（JSON，按 resource 覆盖套餐默认配额，可空） */
    private String quotaOverride;
    private String remark;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getQuotaOverride() {
        return quotaOverride;
    }

    public void setQuotaOverride(String quotaOverride) {
        this.quotaOverride = quotaOverride;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
