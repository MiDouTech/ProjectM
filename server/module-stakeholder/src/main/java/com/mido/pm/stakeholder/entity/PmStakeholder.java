package com.mido.pm.stakeholder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * 干系人（pm_stakeholder）。NPSS 一等公民。
 * category：internal/external；role：sponsor/business/team/finance/regulator/other；
 * power_level/interest_level：1-5；npss_weight：百分比权重。
 */
@TableName("pm_stakeholder")
public class PmStakeholder extends BaseEntity {

    private Long projectId;
    private Long userId;
    private String externalName;
    private String role;
    private String category;
    private Integer powerLevel;
    private Integer interestLevel;
    private BigDecimal npssWeight;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getExternalName() { return externalName; }
    public void setExternalName(String externalName) { this.externalName = externalName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getPowerLevel() { return powerLevel; }
    public void setPowerLevel(Integer powerLevel) { this.powerLevel = powerLevel; }
    public Integer getInterestLevel() { return interestLevel; }
    public void setInterestLevel(Integer interestLevel) { this.interestLevel = interestLevel; }
    public BigDecimal getNpssWeight() { return npssWeight; }
    public void setNpssWeight(BigDecimal npssWeight) { this.npssWeight = npssWeight; }
}
