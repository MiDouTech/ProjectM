package com.mido.pm.org.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 角色字段级权限（sys_field_perm）。access 取 view/edit（见 FieldAccess）。
 * 仅登记需收紧的字段；未登记字段默认可编辑。公共字段见 {@link BaseEntity}。
 */
@TableName("sys_field_perm")
public class SysFieldPerm extends BaseEntity {

    private Long roleId;
    /** 资源标识：task/project */
    private String resource;
    /** 字段键，如 priority/status/assignee */
    private String field;
    /** 访问级别：view 仅查看 / edit 可编辑 */
    private String access;

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

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }
}
