package com.mido.pm.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 平台运营账号（sys_platform_admin）。独立于任何租户的账号体系。
 * status：active/disabled。
 */
@TableName("sys_platform_admin")
public class SysPlatformAdmin extends PlatformBaseEntity {

    private String username;
    /** BCrypt 哈希 */
    private String password;
    private String name;
    /** active/disabled */
    private String status;
    private LocalDateTime lastLoginAt;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
