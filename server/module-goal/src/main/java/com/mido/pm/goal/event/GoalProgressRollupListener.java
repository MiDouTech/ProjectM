package com.mido.pm.goal.event;

import com.mido.pm.common.outbox.DomainEventMessage;
import com.mido.pm.common.tenant.TenantContext;
import com.mido.pm.goal.service.GoalRollupScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 目标进度自动汇总监听：project.status.changed / task.status.changed → 重算对齐到该项目且开启
 * auto_rollup 的 KR。AFTER_COMMIT 捕获租户后交防抖调度器合并重算（单独事务写回，单向项目→KR，无环）。
 */
@Component
public class GoalProgressRollupListener {

    private final GoalRollupScheduler scheduler;

    public GoalProgressRollupListener(GoalRollupScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEventMessage message) {
        if (!(message.payload() instanceof Map<?, ?> payload)) {
            return;
        }
        boolean relevant = "project.status.changed".equals(message.eventType())
                || "task.status.changed".equals(message.eventType());
        if (relevant && payload.get("projectId") instanceof Number id) {
            // 在请求线程捕获租户，传给脱离上下文的汇总线程
            scheduler.schedule(TenantContext.get(), id.longValue());
        }
    }
}
