package com.mido.pm.task.provider;

import com.mido.pm.report.domain.StatusMetaPort;
import com.mido.pm.task.service.WorkItemMetaResolver;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 状态元类别端口的任务域实现：暴露当前租户「已完成」元类别的状态 id 给报表。
 * 实现报表域定义的 {@link StatusMetaPort}（分层不成环 task→report）。
 */
@Component
public class TaskStatusMetaProvider implements StatusMetaPort {

    private final WorkItemMetaResolver metaResolver;

    public TaskStatusMetaProvider(WorkItemMetaResolver metaResolver) {
        this.metaResolver = metaResolver;
    }

    @Override
    public List<Long> doneStatusIds() {
        return metaResolver.doneStatusIds();
    }
}
