package com.mido.pm.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/** 平台账号-角色关联（sys_platform_admin_role）。 */
@TableName("sys_platform_admin_role")
public class SysPlatformAdminRole extends PlatformBaseEntity {

    private Long adminId;
    private Long roleId;

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
