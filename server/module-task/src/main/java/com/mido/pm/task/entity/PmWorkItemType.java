package com.mido.pm.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 工作项类型（pm_work_item_type，租户自配）= 字段集 + 工作流 + 模板。
 * builtin=1 为默认任务类型（不可删）。公共字段见 {@link BaseEntity}。
 */
@TableName("pm_work_item_type")
public class PmWorkItemType extends BaseEntity {

    private String code;
    private String name;
    private String groupName;
    private Integer builtin;
    private Integer sort;
    private String status;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public Integer getBuiltin() { return builtin; }
    public void setBuiltin(Integer builtin) { this.builtin = builtin; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
