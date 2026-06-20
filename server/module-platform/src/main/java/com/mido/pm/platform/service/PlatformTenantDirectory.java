package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.tenant.TenantDirectory;
import com.mido.pm.platform.entity.SysTenant;
import com.mido.pm.platform.mapper.SysTenantMapper;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * TenantDirectory 端口的平台域实现：按租户编码解析、校验可登录。供登录链路（provider）按接口注入。
 */
@Component
public class PlatformTenantDirectory implements TenantDirectory {

    /** 可登录状态：试用与正式可登录；停用/过期/注销拒登。 */
    private static final Set<String> LOGINABLE = Set.of("trial", "active");

    private final SysTenantMapper tenantMapper;

    public PlatformTenantDirectory(SysTenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    @Override
    public Long resolveIdByCode(String code) {
        SysTenant t = tenantMapper.selectOne(Wrappers.<SysTenant>lambdaQuery()
                .eq(SysTenant::getCode, code).last("limit 1"));
        return t == null ? null : t.getId();
    }

    @Override
    public boolean isLoginable(Long tenantId) {
        if (tenantId == null) {
            return false;
        }
        SysTenant t = tenantMapper.selectById(tenantId);
        return t != null && LOGINABLE.contains(t.getStatus());
    }
}
