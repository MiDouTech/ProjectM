package com.mido.pm.calendar.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 日历容器（pm_calendar）。type：personal 我的日程 / meeting 会议安排 / team 团队 / resource 资源。
 * is_default 标记用户默认「我的日程」日历（每用户至多一个）。
 */
@TableName("pm_calendar")
public class PmCalendar extends BaseEntity {

    private String name;
    private String type;
    private Long ownerId;
    private String color;
    private String visibility;
    private Integer isDefault;
    private String subscribeToken;
    private String status;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }
    public String getSubscribeToken() { return subscribeToken; }
    public void setSubscribeToken(String subscribeToken) { this.subscribeToken = subscribeToken; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
