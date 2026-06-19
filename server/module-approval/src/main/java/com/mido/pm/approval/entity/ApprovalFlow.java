package com.mido.pm.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 审批流定义（approval_flow）。mode：fixed/free/conditional；definition 为节点/条件/审批人 JSON。
 */
@TableName("approval_flow")
public class ApprovalFlow extends BaseEntity {

    private String name;
    private String displayName;
    private String bizType;
    private String mode;
    private String definition;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getDefinition() { return definition; }
    public void setDefinition(String definition) { this.definition = definition; }
}
