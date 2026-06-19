package com.mido.pm.goal.event;

import com.mido.pm.common.outbox.DomainEventMessage;
import com.mido.pm.goal.service.GoalService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 监听 task.deleted / project.deleted → 清理指向该对象的对齐链（弱关联，只删关联不动目标）。
 * AFTER_COMMIT：源对象删除事务提交后再清理，避免回滚遗留。
 */
@Component
public class GoalAlignmentCleanupListener {

    private final GoalService goalService;

    public GoalAlignmentCleanupListener(GoalService goalService) {
        this.goalService = goalService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEventMessage message) {
        if (!(message.payload() instanceof Map<?, ?> payload)) {
            return;
        }
        if ("task.deleted".equals(message.eventType()) && payload.get("taskId") instanceof Number id) {
            goalService.removeAlignmentsByTarget("task", id.longValue());
        } else if ("project.deleted".equals(message.eventType())
                && payload.get("projectId") instanceof Number id) {
            goalService.removeAlignmentsByTarget("project", id.longValue());
        }
    }
}
