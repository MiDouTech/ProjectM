package com.mido.pm.collab.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 评论（pm_comment）。可对任务/项目/目标评论；mention 为 @用户 ID 列表(JSON)。
 */
@TableName("pm_comment")
public class PmComment extends BaseEntity {

    private String entityType;
    private Long entityId;
    private Long userId;
    private String content;
    /** @用户 ID 列表(JSON) */
    private String mention;

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMention() { return mention; }
    public void setMention(String mention) { this.mention = mention; }
}
