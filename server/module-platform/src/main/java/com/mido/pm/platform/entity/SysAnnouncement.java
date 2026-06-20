package com.mido.pm.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 平台公告（sys_announcement）。平台域全局表，面向全部租户下发。
 * status：draft 草稿 / published 已发布。level：info/warning。
 */
@TableName("sys_announcement")
public class SysAnnouncement extends PlatformBaseEntity {

    private String title;
    private String content;
    /** info / warning */
    private String level;
    /** draft / published */
    private String status;
    private LocalDateTime publishAt;
    private LocalDateTime expireAt;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPublishAt() {
        return publishAt;
    }

    public void setPublishAt(LocalDateTime publishAt) {
        this.publishAt = publishAt;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }
}
