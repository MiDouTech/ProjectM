package com.mido.pm.stakeholder.purge;

import com.mido.pm.common.tenant.TenantDataPurger;
import org.springframework.stereotype.Component;

/** 干系人域数据清除。 */
@Component
public class StakeholderPurger implements TenantDataPurger {

    private final StakeholderPurgeMapper mapper;

    public StakeholderPurger(StakeholderPurgeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String domain() {
        return "stakeholder";
    }

    @Override
    public long purge(Long tenantId) {
        return mapper.purgeStakeholders(tenantId);
    }
}
