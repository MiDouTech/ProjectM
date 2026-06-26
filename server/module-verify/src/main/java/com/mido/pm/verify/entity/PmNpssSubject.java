package com.mido.pm.verify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * NPSS 评价主体（pm_npss_subject，项目级实例）。从租户模板 {@link PmNpssSubjectTemplate} 物化，项目可覆盖。
 * 成员见 {@link PmNpssSubjectMember}（成员即干系人）；汇总时组内平均后按 weight 加权（npss-rule §3）。
 */
@TableName("pm_npss_subject")
public class PmNpssSubject extends BaseEntity {

    private Long projectId;
    private Long templateId;
    private String name;
    private BigDecimal weight;
    private Integer beneficiary;
    private Integer sort;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public Integer getBeneficiary() { return beneficiary; }
    public void setBeneficiary(Integer beneficiary) { this.beneficiary = beneficiary; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
