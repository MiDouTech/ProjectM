package com.mido.pm.org.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 部门（sys_dept）。parent_id=0 为根。公共字段见 {@link BaseEntity}。
 */
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    private String name;
    private Long parentId;
    /** 部门负责人用户 ID（V18）：动态审批人「部门主管/直属上级」解析用 */
    private Long leaderId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(Long leaderId) {
        this.leaderId = leaderId;
    }
}
