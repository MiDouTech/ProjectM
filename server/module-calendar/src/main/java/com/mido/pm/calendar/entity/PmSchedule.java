package com.mido.pm.calendar.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 日程主记录（pm_schedule）。事件型日程，与任务两套数据。
 * recur_rule / reminder 列本期预留不启用（循环展开、提醒投递为 P1）。
 */
@TableName("pm_schedule")
public class PmSchedule extends BaseEntity {

    private Long calendarId;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer allDay;
    private String location;
    /** RRULE 循环规则(P1)，空=不重复 */
    private String recurRule;
    /** 提醒配置 JSON(P1) */
    private String reminder;
    private Integer allowFeedback;
    private String sourceType;
    private Long sourceId;
    private Long organizerId;
    private String status;

    public Long getCalendarId() { return calendarId; }
    public void setCalendarId(Long calendarId) { this.calendarId = calendarId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getAllDay() { return allDay; }
    public void setAllDay(Integer allDay) { this.allDay = allDay; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getRecurRule() { return recurRule; }
    public void setRecurRule(String recurRule) { this.recurRule = recurRule; }
    public String getReminder() { return reminder; }
    public void setReminder(String reminder) { this.reminder = reminder; }
    public Integer getAllowFeedback() { return allowFeedback; }
    public void setAllowFeedback(Integer allowFeedback) { this.allowFeedback = allowFeedback; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public Long getOrganizerId() { return organizerId; }
    public void setOrganizerId(Long organizerId) { this.organizerId = organizerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
