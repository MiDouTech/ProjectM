package com.mido.pm.goal.usage;

import com.mido.pm.common.tenant.TenantDataPurger;
import com.mido.pm.goal.mapper.GoalPurgeMapper;
import org.springframework.stereotype.Component;

/** 目标域数据清除。 */
@Component
public class GoalPurger implements TenantDataPurger {

    private final GoalPurgeMapper purgeMapper;

    public GoalPurger(GoalPurgeMapper purgeMapper) {
        this.purgeMapper = purgeMapper;
    }

    @Override
    public String domain() {
        return "goal";
    }

    @Override
    public long purge(Long tenantId) {
        return purgeMapper.purgeAlignments(tenantId) + purgeMapper.purgeGoals(tenantId);
    }
}
