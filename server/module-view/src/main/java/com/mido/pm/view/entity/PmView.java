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
    /** 视图渲染类型：kanban/list/table/gantt/calendar；workbench 布局沿用既有用法 */
    private String type;
    /** 视图名（ViewSwitcher 展示；workbench 布局可空） */
    private String name;
    /** 项目级视图所属项目（个人视图为空） */
    private Long projectId;
    /** 视图配置（JSON 字符串，见 ViewConfig） */
    private String config;

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
}
