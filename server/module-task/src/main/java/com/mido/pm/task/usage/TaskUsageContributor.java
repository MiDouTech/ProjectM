package com.mido.pm.task.usage;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.quota.QuotaResources;
import com.mido.pm.common.quota.UsageContributor;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.mapper.PmTaskMapper;
import org.springframework.stereotype.Component;

/** 用量贡献：当前租户任务数（经多租户拦截器按 TenantContext 隔离）。 */
@Component
public class TaskUsageContributor implements UsageContributor {

    private final PmTaskMapper taskMapper;

    public TaskUsageContributor(PmTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @Override
    public String resource() {
        return QuotaResources.TASK;
    }

    @Override
    public long currentCount() {
        Long c = taskMapper.selectCount(Wrappers.<PmTask>lambdaQuery());
        return c == null ? 0L : c;
    }
}
