package com.mido.pm.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 工作项类型-字段绑定（pm_work_item_type_field）。field_key 为系统字段名或自定义 fieldKey。
 * 公共字段见 {@link BaseEntity}。
 */
@TableName("pm_work_item_type_field")
public class PmWorkItemTypeField extends BaseEntity {

    private Long typeId;
    private String fieldKey;
    private Integer required;
    private Integer sort;

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }
    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    public Integer getRequired() { return required; }
    public void setRequired(Integer required) { this.required = required; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
