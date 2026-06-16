package com.mido.pm.collab.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 通知（pm_notification）。channel：inapp 站内信 / wecom 企微（P2）。
 */
@TableName("pm_notification")
public class PmNotification extends BaseEntity {

    public static final String CHANNEL_INAPP = "inapp";

    private Long userId;
    private String type;
    private String title;
    private String payload;
    private Integer isRead;
    private String channel;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
}
