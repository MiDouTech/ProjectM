package com.mido.pm.org.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 角色自定义部门集（sys_role_custom_dept）。当角色某资源数据范围为 custom 时生效。
 * 公共字段见 {@link BaseEntity}。
 */
@TableName("sys_role_custom_dept")
public class SysRoleCustomDept extends BaseEntity {

    private Long roleId;
    private Long deptId;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }
}
