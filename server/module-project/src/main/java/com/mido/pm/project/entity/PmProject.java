package com.mido.pm.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目（pm_project）。公共字段见 {@link BaseEntity}。
 */
@TableName("pm_project")
public class PmProject extends BaseEntity {

    private String code;
    private String name;
    private String description;
    /** S/I/O */
    private String category;
    /** O 细分：常规运营/定向整改/专项督办 */
    private String subCategory;
    private Long templateId;
    private Long leaderId;
    /** 归属部门（=leader 部门）：数据范围按部门过滤（V9） */
    private Long deptId;
    private String status;
    private Long workflowId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private BigDecimal actualCost;
    /** NPSS 应启动日（结案+6~12月） */
    private LocalDate valueReviewDueDate;
    private LocalDateTime pmoRegisteredAt;
    private Integer archived;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public Long getLeaderId() { return leaderId; }
    public void setLeaderId(Long leaderId) { this.leaderId = leaderId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getWorkflowId() { return workflowId; }
    public void setWorkflowId(Long workflowId) { this.workflowId = workflowId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public BigDecimal getActualCost() { return actualCost; }
    public void setActualCost(BigDecimal actualCost) { this.actualCost = actualCost; }
    public LocalDate getValueReviewDueDate() { return valueReviewDueDate; }
    public void setValueReviewDueDate(LocalDate valueReviewDueDate) { this.valueReviewDueDate = valueReviewDueDate; }
    public LocalDateTime getPmoRegisteredAt() { return pmoRegisteredAt; }
    public void setPmoRegisteredAt(LocalDateTime pmoRegisteredAt) { this.pmoRegisteredAt = pmoRegisteredAt; }
    public Integer getArchived() { return archived; }
    public void setArchived(Integer archived) { this.archived = archived; }
}
