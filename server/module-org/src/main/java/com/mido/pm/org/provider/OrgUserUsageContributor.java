package com.mido.pm.org.provider;

import com.mido.pm.common.quota.QuotaResources;
import com.mido.pm.common.quota.UsageContributor;
import com.mido.pm.org.entity.SysUser;
import com.mido.pm.org.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Component;

/** 用量贡献：当前租户成员数（经多租户拦截器按 TenantContext 隔离）。 */
@Component
public class OrgUserUsageContributor implements UsageContributor {

    private final SysUserMapper userMapper;

    public OrgUserUsageContributor(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public String resource() {
        return QuotaResources.USER;
    }

    @Override
    public long currentCount() {
        Long c = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery());
        return c == null ? 0L : c;
    }
}
