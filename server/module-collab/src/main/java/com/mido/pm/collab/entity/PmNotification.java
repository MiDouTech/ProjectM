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
    private String bizType;
    private Long bizId;
    private String title;
    private String payload;
    private String link;
    private Integer isRead;
    private String channel;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
}
