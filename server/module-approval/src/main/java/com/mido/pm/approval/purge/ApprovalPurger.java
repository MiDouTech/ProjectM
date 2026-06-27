package com.mido.pm.approval.purge;

import com.mido.pm.common.tenant.TenantDataPurger;
import org.springframework.stereotype.Component;

/** 审批域数据清除。 */
@Component
public class ApprovalPurger implements TenantDataPurger {

    private final ApprovalPurgeMapper mapper;

    public ApprovalPurger(ApprovalPurgeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String domain() {
        return "approval";
    }

    @Override
    public long purge(Long tenantId) {
        return mapper.purgeTasks(tenantId)
                + mapper.purgeInstances(tenantId)
                + mapper.purgeForms(tenantId)
                + mapper.purgeFlows(tenantId);
    }
}
