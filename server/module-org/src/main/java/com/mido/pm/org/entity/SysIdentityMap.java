package com.mido.pm.org.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mido.pm.common.entity.BaseEntity;

/**
 * 外部身份映射（sys_identity_map）。企微 userid ↔ 本地 user 解耦，SSO/同步预留。
 * 公共字段见 {@link BaseEntity}。
 */
@TableName("sys_identity_map")
public class SysIdentityMap extends BaseEntity {

    private Long userId;
    private String provider;
    private String externalId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
}
