package com.mido.pm.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 套餐功能开关（sys_plan_feature）。一个套餐对多条功能开关，决定订阅该套餐的租户可用哪些功能。
 * feature_code 取自 common 的 FeatureCodes。
 */
@TableName("sys_plan_feature")
public class SysPlanFeature extends PlatformBaseEntity {

    private Long planId;
    private String featureCode;
    private Integer enabled;

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public void setFeatureCode(String featureCode) {
        this.featureCode = featureCode;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
}
