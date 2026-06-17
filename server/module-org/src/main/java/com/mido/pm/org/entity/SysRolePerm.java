package com.mido.pm.org.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 角色-权限码（sys_role_perm）。公共字段见 {@link BaseEntity}。
 */
@TableName("sys_role_perm")
public class SysRolePerm extends BaseEntity {

    private Long roleId;
    private String permCode;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getPermCode() {
        return permCode;
    }

    public void setPermCode(String permCode) {
        this.permCode = permCode;
    }
}
