package com.mido.pm.calendar.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/** 日程提醒发送去重日志（pm_schedule_reminder_log）。 */
@TableName("pm_schedule_reminder_log")
public class PmScheduleReminderLog extends BaseEntity {

    private Long scheduleId;
    private Integer remindMinute;
    private LocalDateTime sentAt;

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public Integer getRemindMinute() { return remindMinute; }
    public void setRemindMinute(Integer remindMinute) { this.remindMinute = remindMinute; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
