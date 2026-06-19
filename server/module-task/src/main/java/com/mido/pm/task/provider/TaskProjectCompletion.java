package com.mido.pm.task.provider;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.goal.domain.ProjectCompletionPort;
import com.mido.pm.task.domain.TaskStatus;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.mapper.PmTaskMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 项目完成率端口的任务域实现：完成率 = (已完成 + 已验收) 任务数 / 总任务数 × 100。
 * 实现目标域定义的 {@link ProjectCompletionPort}（动态汇总用），分层不成环（task→goal）。
 */
@Component
public class TaskProjectCompletion implements ProjectCompletionPort {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PmTaskMapper taskMapper;

    public TaskProjectCompletion(PmTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
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
        Long done = taskMapper.selectCount(Wrappers.<PmTask>lambdaQuery()
                .eq(PmTask::getProjectId, projectId)
                .in(PmTask::getStatus, TaskStatus.DONE.getCode(), TaskStatus.ACCEPTED.getCode()));
        return BigDecimal.valueOf(done == null ? 0 : done)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
