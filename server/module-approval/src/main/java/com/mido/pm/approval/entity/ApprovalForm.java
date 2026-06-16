package com.mido.pm.approval.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 审批表单定义（approval_form）。schema 为保留字，列名加反引号。
 */
@TableName("approval_form")
public class ApprovalForm extends BaseEntity {

    private String name;
    private String bizType;
    /** 表单 schema（JSON），schema 为 MySQL 保留字 */
    @TableField("`schema`")
    private String schema;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }
}
