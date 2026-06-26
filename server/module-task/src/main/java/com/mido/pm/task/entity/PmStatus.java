package com.mido.pm.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 状态库（pm_status，租户自配）。每状态归约到 meta_category(未开始/进行中/已完成) 保统计口径。
 * builtin=1 为内置三态，不可删。公共字段见 {@link BaseEntity}。
 */
@TableName("pm_status")
public class PmStatus extends BaseEntity {

    private String name;
    private String color;
    private String metaCategory;
    private String groupName;
    private Integer sort;
    private Integer builtin;
    private String status;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getMetaCategory() { return metaCategory; }
    public void setMetaCategory(String metaCategory) { this.metaCategory = metaCategory; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getBuiltin() { return builtin; }
    public void setBuiltin(Integer builtin) { this.builtin = builtin; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
