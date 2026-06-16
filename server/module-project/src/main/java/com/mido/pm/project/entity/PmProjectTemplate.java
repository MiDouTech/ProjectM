package com.mido.pm.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 项目模板（pm_project_template）。内置 5 套的种子与「按模板建项目」在 Step 2-2 实现。
 * 公共字段见 {@link BaseEntity}。
 */
@TableName("pm_project_template")
public class PmProjectTemplate extends BaseEntity {

    private String name;
    private String category;
    private String subCategory;
    private String description;
    private Integer isBuiltin;
    /** 阶段/任务骨架/默认干系人权重/默认审批流/默认字段（JSON） */
    private String config;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getIsBuiltin() { return isBuiltin; }
    public void setIsBuiltin(Integer isBuiltin) { this.isBuiltin = isBuiltin; }
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
}
