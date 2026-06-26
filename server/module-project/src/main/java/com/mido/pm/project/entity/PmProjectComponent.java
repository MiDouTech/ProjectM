package com.mido.pm.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 项目已安装组件（pm_project_component）。同一组件可多实例（看板/表格）。公共字段见 {@link BaseEntity}。
 */
@TableName("pm_project_component")
public class PmProjectComponent extends BaseEntity {

    private Long projectId;
    private String componentCode;
    private String name;
    private Integer sort;
    private Integer enabled;
    private String config;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getComponentCode() { return componentCode; }
    public void setComponentCode(String componentCode) { this.componentCode = componentCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
}
