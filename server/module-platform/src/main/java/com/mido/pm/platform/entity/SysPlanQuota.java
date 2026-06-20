package com.mido.pm.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 套餐配额项（sys_plan_quota）。一个套餐对多条配额。
 * resource：user/project/storage_mb/task 等；limitValue：-1 表示不限。
 */
@TableName("sys_plan_quota")
public class SysPlanQuota extends PlatformBaseEntity {

    private Long planId;
    /** 配额资源：user/project/storage_mb/task... */
    private String resource;
    /** 上限值，-1 表示不限 */
    private Long limitValue;

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Long getLimitValue() {
        return limitValue;
    }

    public void setLimitValue(Long limitValue) {
        this.limitValue = limitValue;
    }
}
