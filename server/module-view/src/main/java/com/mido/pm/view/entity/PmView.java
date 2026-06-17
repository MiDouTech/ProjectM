package com.mido.pm.view.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 视图配置（pm_view）。scope 区分用途（如 workbench），owner_id 为归属用户，config 为 JSON。
 */
@TableName("pm_view")
public class PmView extends BaseEntity {

    /** 用途范围：workbench / project / task ... */
    private String scope;
    /** 归属用户 ID（个人视图） */
    private Long ownerId;
    /** 视图子类型（可空） */
    private String type;
    /** 视图配置（JSON 字符串） */
    private String config;

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
}
