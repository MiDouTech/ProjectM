package com.mido.pm.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/** 平台角色-权限码关联（sys_platform_role_perm）。perm_code 取自 {@code PlatformPerms}。 */
@TableName("sys_platform_role_perm")
public class SysPlatformRolePerm extends PlatformBaseEntity {

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
