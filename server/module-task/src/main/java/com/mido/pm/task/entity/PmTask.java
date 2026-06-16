package com.mido.pm.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 任务（pm_task）。parent_id 自关联子任务；is_milestone 里程碑标记。
 * 依赖/工时/循环规则(est_hours/actual_hours/recur_rule)字段就位但 P0 不操作（留 P1）。
 */
@TableName("pm_task")
public class PmTask extends BaseEntity {

    private Long projectId;
    private Long parentId;
    private String title;
    private String description;
    private Long assigneeId;
    private String status;
    private Integer priority;
    private String stage;
    private LocalDate startDate;
    private LocalDate dueDate;
    private Integer isMilestone;
    private String recurRule;
    private BigDecimal estHours;
    private BigDecimal actualHours;
    private String customFields;
    private String aiSource;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Integer getIsMilestone() { return isMilestone; }
    public void setIsMilestone(Integer isMilestone) { this.isMilestone = isMilestone; }
    public String getRecurRule() { return recurRule; }
    public void setRecurRule(String recurRule) { this.recurRule = recurRule; }
    public BigDecimal getEstHours() { return estHours; }
    public void setEstHours(BigDecimal estHours) { this.estHours = estHours; }
    public BigDecimal getActualHours() { return actualHours; }
    public void setActualHours(BigDecimal actualHours) { this.actualHours = actualHours; }
    public String getCustomFields() { return customFields; }
    public void setCustomFields(String customFields) { this.customFields = customFields; }
    public String getAiSource() { return aiSource; }
    public void setAiSource(String aiSource) { this.aiSource = aiSource; }
}
