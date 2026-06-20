package com.mido.pm.goal.usage;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.tenant.TenantDataExporter;
import com.mido.pm.goal.entity.PmGoal;
import com.mido.pm.goal.mapper.PmGoalMapper;
import org.springframework.stereotype.Component;

/** 导出当前租户目标数据。 */
@Component
public class GoalExporter implements TenantDataExporter {

    private final PmGoalMapper goalMapper;

    public GoalExporter(PmGoalMapper goalMapper) {
        this.goalMapper = goalMapper;
    }

    @Override
    public String domain() {
        return "goals";
    }

    @Override
    public Object exportData() {
        return goalMapper.selectList(Wrappers.<PmGoal>lambdaQuery());
    }
}
