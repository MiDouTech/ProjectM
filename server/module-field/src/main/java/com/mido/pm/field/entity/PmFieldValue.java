package com.mido.pm.field.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 自定义字段值（pm_field_value，EAV）。以 (entity_type, entity_id) 挂载于业务实体。
 * value 统一文本存储；多选/用户型存 JSON 字符串。
 */
@TableName("pm_field_value")
public class PmFieldValue extends BaseEntity {

    /** 字段定义 id（pm_field_def.id） */
    private Long fieldId;
    /** 实体类型：task / project */
    private String entityType;
    /** 实体 id */
    private Long entityId;
    /** 字段值（统一文本；多选/用户型存 JSON 字符串） */
    private String value;

    public Long getFieldId() { return fieldId; }
    public void setFieldId(Long fieldId) { this.fieldId = fieldId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
