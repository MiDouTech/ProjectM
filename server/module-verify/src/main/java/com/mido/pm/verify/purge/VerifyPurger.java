package com.mido.pm.verify.purge;

import com.mido.pm.common.tenant.TenantDataPurger;
import org.springframework.stereotype.Component;

/** 验收(NPSS)域数据清除。 */
@Component
public class VerifyPurger implements TenantDataPurger {

    private final VerifyPurgeMapper mapper;

    public VerifyPurger(VerifyPurgeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String domain() {
        return "verify";
    }

    @Override
    public long purge(Long tenantId) {
        return mapper.purgeScores(tenantId)
                + mapper.purgeSubjectMembers(tenantId)
                + mapper.purgeReviews(tenantId)
                + mapper.purgeSubjects(tenantId)
                + mapper.purgeSubjectTemplates(tenantId)
                + mapper.purgeResultVerifies(tenantId);
    }
}
