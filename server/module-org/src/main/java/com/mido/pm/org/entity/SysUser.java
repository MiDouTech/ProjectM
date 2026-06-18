package com.mido.pm.org.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 用户（sys_user）。公共字段见 {@link BaseEntity}。
 */
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    /** 手机号：登录账号（全局唯一），手机号/用户名双登录 */
    private String phone;
    private String name;
    /** 头像：附件 ID（经 /attachments/{id}/download-url 取限时图片地址），可空 */
    private String avatar;
    private String password;
    private Long deptId;
    /** 职级（如 L1/L2/L3），立项职级 guard 依赖 */
    private String jobLevel;
    private String status;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getJobLevel() {
        return jobLevel;
    }

    public void setJobLevel(String jobLevel) {
        this.jobLevel = jobLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
