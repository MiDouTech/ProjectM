package com.mido.pm.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 附件（pm_attachment）。通用挂载：entity_type/entity_id（如 task/项目/目标）。
 * oss_key 为对象存储键，仅服务端可见，绝不外泄前端（下载走预签名 URL）。
 */
@TableName("pm_attachment")
public class PmAttachment extends BaseEntity {

    private String entityType;
    private Long entityId;
    /** 原始文件名（展示用） */
    private String name;
    /** 对象存储键（服务端内部使用，不外泄） */
    private String ossKey;
    /** 字节数 */
    private Long size;

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOssKey() { return ossKey; }
    public void setOssKey(String ossKey) { this.ossKey = ossKey; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
}
