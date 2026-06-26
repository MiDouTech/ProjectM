package com.mido.pm.verify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * NPSS 评价主体模板（pm_npss_subject_template，租户级）。立项/发起评价时物化为项目级 {@link PmNpssSubject}。
 * beneficiary=1 标记受益方，用于"受益方合计≥50%"硬校验；启用主体权重合计须=100%。
 */
@TableName("pm_npss_subject_template")
public class PmNpssSubjectTemplate extends BaseEntity {

    private String name;
    private BigDecimal weight;
    private Integer beneficiary;
    private Integer sort;
    private Integer enabled;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public Integer getBeneficiary() { return beneficiary; }
    public void setBeneficiary(Integer beneficiary) { this.beneficiary = beneficiary; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}
