package com.mido.pm.calendar.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 日程参与人 + RSVP（pm_schedule_participant）。
 * role：organizer/required/optional；rsvp_status：pending/accepted/tentative/declined。
 */
@TableName("pm_schedule_participant")
public class PmScheduleParticipant extends BaseEntity {

    private Long scheduleId;
    private Long userId;
    private String externalName;
    private String role;
    private String rsvpStatus;

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getExternalName() { return externalName; }
    public void setExternalName(String externalName) { this.externalName = externalName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getRsvpStatus() { return rsvpStatus; }
    public void setRsvpStatus(String rsvpStatus) { this.rsvpStatus = rsvpStatus; }
}
