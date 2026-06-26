package com.mido.pm.task.provider;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.goal.domain.ProjectCompletionPort;
import com.mido.pm.task.domain.TaskStatus;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.mapper.PmTaskMapper;
import com.mido.pm.task.service.WorkItemMetaResolver;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 项目完成率端口的任务域实现：完成率 = 「已完成」元类别任务数 / 总任务数 × 100。
 * 实现目标域定义的 {@link ProjectCompletionPort}（动态汇总用），分层不成环（task→goal）。
 *
 * <p>翻转读方：完成判定以状态库「元类别=已完成」为准（含自定义状态）；未配置状态库的租户
 * （status_id 为空）回落到字符串终态（已完成/已验收），行为不变。</p>
 */
@Component
public class TaskProjectCompletion implements ProjectCompletionPort {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PmTaskMapper taskMapper;
    private final WorkItemMetaResolver metaResolver;

    public TaskProjectCompletion(PmTaskMapper taskMapper, WorkItemMetaResolver metaResolver) {
        this.taskMapper = taskMapper;
        this.metaResolver = metaResolver;
    }

    @Override
    public BigDecimal completionRate(Long projectId) {
        if (projectId == null) {
            return BigDecimal.ZERO;
        }
        Long total = taskMapper.selectCount(Wrappers.<PmTask>lambdaQuery()
                .eq(PmTask::getProjectId, projectId));
        if (total == null || total == 0) {
            return BigDecimal.ZERO;
        }
        List<Long> doneIds = metaResolver.doneStatusIds();
        Long done = taskMapper.selectCount(Wrappers.<PmTask>lambdaQuery()
                .eq(PmTask::getProjectId, projectId)
                .and(w -> {
                    if (!doneIds.isEmpty()) {
                        // 有状态库：元类别=已完成 的状态；status_id 为空的存量行回落字符串终态
                        w.in(PmTask::getStatusId, doneIds)
                                .or(o -> o.isNull(PmTask::getStatusId)
                                        .in(PmTask::getStatus, TaskStatus.DONE.getCode(), TaskStatus.ACCEPTED.getCode()));
                    } else {
                        // 未配置状态库：纯字符串终态（行为不变）
                        w.in(PmTask::getStatus, TaskStatus.DONE.getCode(), TaskStatus.ACCEPTED.getCode());
                    }
                }));
        return BigDecimal.valueOf(done == null ? 0 : done)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
