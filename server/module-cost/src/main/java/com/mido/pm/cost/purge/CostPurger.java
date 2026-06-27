package com.mido.pm.cost.purge;

import com.mido.pm.common.tenant.TenantDataPurger;
import org.springframework.stereotype.Component;

/** 费用域数据清除。 */
@Component
public class CostPurger implements TenantDataPurger {

    private final CostPurgeMapper mapper;

    public CostPurger(CostPurgeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String domain() {
        return "cost";
    }

    @Override
    public long purge(Long tenantId) {
        return mapper.purgeCosts(tenantId);
    }
}
