package com.mido.pm.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 平台角色（sys_platform_role）。内置：super_admin/operator/support/finance/viewer。
 */
@TableName("sys_platform_role")
public class SysPlatformRole extends PlatformBaseEntity {

    private String name;
    private String code;
    private String remark;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
