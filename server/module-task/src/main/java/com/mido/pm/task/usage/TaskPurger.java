package com.mido.pm.task.usage;

import com.mido.pm.common.tenant.TenantDataPurger;
import com.mido.pm.task.mapper.TaskPurgeMapper;
import org.springframework.stereotype.Component;

/** 任务域数据清除。 */
@Component
public class TaskPurger implements TenantDataPurger {

    private final TaskPurgeMapper purgeMapper;

    public TaskPurger(TaskPurgeMapper purgeMapper) {
        this.purgeMapper = purgeMapper;
    }

    @Override
    public String domain() {
        return "task";
    }

    @Override
    public long purge(Long tenantId) {
        return purgeMapper.purgeDependencies(tenantId)
                + purgeMapper.purgeWorkHours(tenantId)
                + purgeMapper.purgeTasks(tenantId);
    }
}
