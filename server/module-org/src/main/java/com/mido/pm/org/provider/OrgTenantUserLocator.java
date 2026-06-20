package com.mido.pm.org.provider;

import com.mido.pm.common.tenant.TenantUserLocator;
import com.mido.pm.org.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

/**
 * TenantUserLocator 的本地实现（读 sys_user）。置于 module-org 以避免 platform 直查业务表。
 * 供平台域「模拟登录」定位某租户的目标用户。
 */
@Component
public class OrgTenantUserLocator implements TenantUserLocator {

    private final SysUserMapper userMapper;

    public OrgTenantUserLocator(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Long primaryUserId(Long tenantId) {
        return tenantId == null ? null : userMapper.selectPrimaryUserId(tenantId);
    }
}
