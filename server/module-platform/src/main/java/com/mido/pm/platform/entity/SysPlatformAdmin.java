package com.mido.pm.platform.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
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
    /** 首次登录需强制改密（1=是） */
    private Boolean mustChangePassword;
    /** 连续登录失败次数 */
    private Integer failCount;
    /** 锁定到期时间（为空或已过=未锁）。ALWAYS 策略确保登录成功时能把锁定清空(null)。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime lockedUntil;
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

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
