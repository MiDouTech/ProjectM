package com.mido.pm.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 工时（pm_work_hour）。kind：est 预估 / actual 实际；category：设计/研发/文档/测试/其他。
 */
@TableName("pm_work_hour")
public class PmWorkHour extends BaseEntity {

    private Long taskId;
    private Long userId;
    private String kind;
    private String category;
    private LocalDate workDate;
    private BigDecimal hours;
    private String remark;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public BigDecimal getHours() { return hours; }
    public void setHours(BigDecimal hours) { this.hours = hours; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
