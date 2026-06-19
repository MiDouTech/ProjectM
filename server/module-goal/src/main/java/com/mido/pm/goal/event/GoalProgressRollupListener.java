package com.mido.pm.goal.event;

import com.mido.pm.common.outbox.DomainEventMessage;
import com.mido.pm.goal.service.GoalRollupService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 目标进度自动汇总监听：project.status.changed / task.status.changed → 重算对齐到该项目且开启
 * auto_rollup 的 KR。AFTER_COMMIT：源事务提交后再汇总，单独事务写回（单向，项目→KR，无环）。
 */
@Component
public class GoalProgressRollupListener {

    private final GoalRollupService rollupService;

    public GoalProgressRollupListener(GoalRollupService rollupService) {
        this.rollupService = rollupService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEventMessage message) {
        if (!(message.payload() instanceof Map<?, ?> payload)) {
            return;
        }
        boolean relevant = "project.status.changed".equals(message.eventType())
                || "task.status.changed".equals(message.eventType());
        if (relevant && payload.get("projectId") instanceof Number id) {
            rollupService.recomputeForProject(id.longValue());
        }
    }
}
