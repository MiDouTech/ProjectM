package com.mido.pm.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 优先级模式（pm_priority_mode，租户自配）。builtin=1 不可删。公共字段见 {@link BaseEntity}。
 */
@TableName("pm_priority_mode")
public class PmPriorityMode extends BaseEntity {

    private String name;
    private String remark;
    private Integer builtin;
    private String status;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getBuiltin() { return builtin; }
    public void setBuiltin(Integer builtin) { this.builtin = builtin; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
