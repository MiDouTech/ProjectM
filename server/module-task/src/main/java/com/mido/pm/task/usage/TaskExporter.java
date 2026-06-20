package com.mido.pm.task.usage;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.tenant.TenantDataExporter;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.mapper.PmTaskMapper;
import org.springframework.stereotype.Component;

/** 导出当前租户任务数据。 */
@Component
public class TaskExporter implements TenantDataExporter {

    private final PmTaskMapper taskMapper;

    public TaskExporter(PmTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @Override
    public String domain() {
        return "tasks";
    }

    @Override
    public Object exportData() {
        return taskMapper.selectList(Wrappers.<PmTask>lambdaQuery());
    }
}
