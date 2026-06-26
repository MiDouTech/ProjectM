package com.mido.pm.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 项目角色（pm_project_role，租户自配）。成员表 project_role 引用本表 code。
 * builtin=1 为内置（管理员/普通成员/只读成员），不可删除。公共字段见 {@link BaseEntity}。
 */
@TableName("pm_project_role")
public class PmProjectRole extends BaseEntity {

    private String code;
    private String name;
    private Integer builtin;
    private Integer sort;
    private String status;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getBuiltin() {
        return builtin;
    }

    public void setBuiltin(Integer builtin) {
        this.builtin = builtin;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
