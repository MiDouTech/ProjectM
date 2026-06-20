package com.mido.pm.org.provider;

import com.mido.pm.common.tenant.TenantDataPurger;
import com.mido.pm.org.mapper.OrgPurgeMapper;
import org.springframework.stereotype.Component;

/** 组织域数据清除。 */
@Component
public class OrgPurger implements TenantDataPurger {

    private final OrgPurgeMapper purgeMapper;

    public OrgPurger(OrgPurgeMapper purgeMapper) {
        this.purgeMapper = purgeMapper;
    }

    @Override
    public String domain() {
        return "org";
    }

    @Override
    public long purge(Long tenantId) {
        long n = 0;
        n += purgeMapper.purgeUserRoles(tenantId);
        n += purgeMapper.purgeRolePerms(tenantId);
        n += purgeMapper.purgeRoleDataScopes(tenantId);
        n += purgeMapper.purgeIdentityMaps(tenantId);
        n += purgeMapper.purgeApiKeys(tenantId);
        n += purgeMapper.purgeRoles(tenantId);
        n += purgeMapper.purgeDepts(tenantId);
        n += purgeMapper.purgeUsers(tenantId);
        return n;
    }
}
