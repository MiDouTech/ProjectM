package com.mido.pm.view.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 一级模块导航编排（pm_module_nav）。租户对某模块顶部导航组件的增删/排序/改名/显隐；
 * parent_code 空=二级菜单，非空=挂父组件下的三级。空配置→后端回落内置默认（WorkspaceCatalog）。
 */
@TableName("pm_module_nav")
public class PmModuleNav extends BaseEntity {

    private String module;
    private String componentCode;
    private String parentCode;
    private String displayName;
    private String icon;
    private Integer sort;
    private Integer enabled;

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getComponentCode() { return componentCode; }
    public void setComponentCode(String componentCode) { this.componentCode = componentCode; }
    public String getParentCode() { return parentCode; }
    public void setParentCode(String parentCode) { this.parentCode = parentCode; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}
