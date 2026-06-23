package com.mido.pm.calendar.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/** 日历资源（pm_calendar_resource）。type：room 会议室 / device 设备。 */
@TableName("pm_calendar_resource")
public class PmCalendarResource extends BaseEntity {

    private String name;
    private String type;
    private Integer capacity;
    private String location;
    private String status;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
