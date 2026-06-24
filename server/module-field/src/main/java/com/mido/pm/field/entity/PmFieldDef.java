package com.mido.pm.field.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 自定义字段定义（pm_field_def）。租户自配，scope=task/project。
 * select/multi_select 选项以 JSON 存于 options（[{value,label}]）。
 */
@TableName("pm_field_def")
public class PmFieldDef extends BaseEntity {

    /** 作用域：task / project */
    private String scope;
    /** 字段标识（租户内 scope 下唯一） */
    private String fieldKey;
    /** 显示名 */
    private String name;
    /** 类型：text/number/date/select/multi_select/checkbox/user */
    private String type;
    /** 选项 JSON（[{value,label}]，select/multi_select 用） */
    private String options;
    /** 是否必填：0 否 1 是 */
    private Integer required;
    /** 展示排序（升序） */
    private Integer sortNo;
    /** 是否启用：0 停用 1 启用 */
    private Integer enabled;

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    public Integer getRequired() { return required; }
    public void setRequired(Integer required) { this.required = required; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}
