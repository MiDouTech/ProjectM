package com.mido.pm.briefing.purge;

import com.mido.pm.common.tenant.TenantDataPurger;
import org.springframework.stereotype.Component;

/** 简报域数据清除。 */
@Component
public class BriefingPurger implements TenantDataPurger {

    private final BriefingPurgeMapper mapper;

    public BriefingPurger(BriefingPurgeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String domain() {
        return "briefing";
    }

    @Override
    public long purge(Long tenantId) {
        return mapper.purgeRecipients(tenantId)
                + mapper.purgeReviews(tenantId)
                + mapper.purgeIssues(tenantId)
                + mapper.purgeAssignments(tenantId)
                + mapper.purgeBriefings(tenantId)
                + mapper.purgeTemplates(tenantId);
    }
}
