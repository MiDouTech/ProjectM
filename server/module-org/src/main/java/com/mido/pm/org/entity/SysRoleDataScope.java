package com.mido.pm.org.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 角色数据范围（sys_role_data_scope）。scope 取 self/dept/dept_and_sub/all/custom。
 * 公共字段见 {@link BaseEntity}。
 */
@TableName("sys_role_data_scope")
public class SysRoleDataScope extends BaseEntity {

    private Long roleId;
    /** 资源标识（对应业务资源，如 user/project） */
    private String resource;
    private String scope;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
