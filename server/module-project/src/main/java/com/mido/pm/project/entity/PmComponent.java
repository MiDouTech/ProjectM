package com.mido.pm.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 组件库（pm_component，catalog）：项目可安装的视图组件目录。公共字段见 {@link BaseEntity}。
 */
@TableName("pm_component")
public class PmComponent extends BaseEntity {

    private String code;
    private String name;
    private String icon;
    private Integer multiInstance;
    private Integer builtin;
    private Integer sort;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getMultiInstance() { return multiInstance; }
    public void setMultiInstance(Integer multiInstance) { this.multiInstance = multiInstance; }
    public Integer getBuiltin() { return builtin; }
    public void setBuiltin(Integer builtin) { this.builtin = builtin; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
