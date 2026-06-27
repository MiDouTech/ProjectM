package com.mido.pm.change.purge;

import com.mido.pm.common.tenant.TenantDataPurger;
import org.springframework.stereotype.Component;

/** 变更中心域数据清除。 */
@Component
public class ChangePurger implements TenantDataPurger {

    private final ChangePurgeMapper mapper;

    public ChangePurger(ChangePurgeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String domain() {
        return "change";
    }

    @Override
    public long purge(Long tenantId) {
        return mapper.purgeRequests(tenantId) + mapper.purgePolicies(tenantId);
    }
}
