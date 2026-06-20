package com.mido.pm.org.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 开放平台 API Key（sys_api_key）。租户业务表（带 tenant_id，租户隔离）。
 * key 绑定某用户：携带本 key 调用 OpenAPI 时等同该用户身份（继承其权限与数据范围）。
 * 仅存 key 的 SHA-256（key_hash 唯一，供鉴权查找）与前缀（展示），明文仅创建时返回一次。
 */
@TableName("sys_api_key")
public class SysApiKey extends BaseEntity {

    /** 绑定的用户 ID（鉴权时以该用户身份装配上下文） */
    private Long userId;
    private String name;
    /** key 的 SHA-256 十六进制（鉴权查找用，唯一） */
    private String keyHash;
    /** key 前缀（如 mk_xxxxxxxx，仅展示） */
    private String keyPrefix;
    /** active / disabled */
    private String status;
    private LocalDateTime lastUsedAt;
    /** 到期时间，空表示长期有效 */
    private LocalDateTime expireAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }
}
