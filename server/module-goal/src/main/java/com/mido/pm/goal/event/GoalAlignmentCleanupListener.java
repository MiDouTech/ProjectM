package com.mido.pm.goal.event;

import com.mido.pm.common.outbox.DomainEventMessage;
import com.mido.pm.goal.service.GoalService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 监听 task.deleted → 清理指向该任务的对齐链（弱关联，只删关联不动目标）。
 * 项目删除当前无 project.deleted 事件，故项目侧对齐链清理暂不覆盖（悬挂 target_id 无害，不级联删目标）。
 * AFTER_COMMIT：任务删除事务提交后再清理。
 */
@Component
public class GoalAlignmentCleanupListener {

    private final GoalService goalService;

    public GoalAlignmentCleanupListener(GoalService goalService) {
        this.goalService = goalService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEventMessage message) {
        if (!"task.deleted".equals(message.eventType())) {
            return;
        }
        if (message.payload() instanceof Map<?, ?> payload
                && payload.get("taskId") instanceof Number id) {
            goalService.removeAlignmentsByTarget("task", id.longValue());
        }
    }
}
