package com.mido.pm.calendar.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.time.LocalDate;

/**
 * 循环日程例外（pm_schedule_exception）。
 * action：cancel 取消该次 / modify 改期；override 为 modify 的覆盖内容 JSON。
 */
@TableName("pm_schedule_exception")
public class PmScheduleException extends BaseEntity {

    private Long scheduleId;
    private LocalDate occurDate;
    private String action;
    private String override;

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public LocalDate getOccurDate() { return occurDate; }
    public void setOccurDate(LocalDate occurDate) { this.occurDate = occurDate; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getOverride() { return override; }
    public void setOverride(String override) { this.override = override; }
}
