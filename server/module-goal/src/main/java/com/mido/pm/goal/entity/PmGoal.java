package com.mido.pm.goal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * 目标/KR（pm_goal）。type：objective 目标 / kr 关键结果；parent_id 自关联成目标树（KR 挂在目标下）。
 * 量化指标：metric_start/target/current + progress（由 GoalProgress 计算，非手填）。
 */
@TableName("pm_goal")
public class PmGoal extends BaseEntity {

    private String title;
    private String type;
    private Long parentId;
    private Long ownerId;
    private String period;
    private String metricUnit;
    private BigDecimal metricStart;
    private BigDecimal metricTarget;
    private BigDecimal metricCurrent;
    private BigDecimal progress;
    /** KR 进度是否自动汇总对齐项目完成率：1 是 / 0 否 */
    private Integer autoRollup;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getMetricUnit() { return metricUnit; }
    public void setMetricUnit(String metricUnit) { this.metricUnit = metricUnit; }
    public BigDecimal getMetricStart() { return metricStart; }
    public void setMetricStart(BigDecimal metricStart) { this.metricStart = metricStart; }
    public BigDecimal getMetricTarget() { return metricTarget; }
    public void setMetricTarget(BigDecimal metricTarget) { this.metricTarget = metricTarget; }
    public BigDecimal getMetricCurrent() { return metricCurrent; }
    public void setMetricCurrent(BigDecimal metricCurrent) { this.metricCurrent = metricCurrent; }
    public BigDecimal getProgress() { return progress; }
    public void setProgress(BigDecimal progress) { this.progress = progress; }
    public Integer getAutoRollup() { return autoRollup; }
    public void setAutoRollup(Integer autoRollup) { this.autoRollup = autoRollup; }
}
