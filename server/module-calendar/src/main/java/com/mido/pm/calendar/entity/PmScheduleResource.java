package com.mido.pm.calendar.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/** 日程资源占用（pm_schedule_resource）。 */
@TableName("pm_schedule_resource")
public class PmScheduleResource extends BaseEntity {

    private Long scheduleId;
    private Long resourceId;

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
}
