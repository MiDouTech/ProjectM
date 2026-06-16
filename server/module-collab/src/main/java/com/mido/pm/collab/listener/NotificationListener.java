package com.mido.pm.collab.listener;

import com.mido.pm.common.outbox.DomainEventMessage;
import com.mido.pm.provider.message.MessageProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

/**
 * 通知监听器（事件驱动）：订阅领域事件 → 解析收件人 → 经 {@link MessageProvider} 发通知。
 * 业务层不直接调通知；通道(inapp/wecom)由 MessageProvider 实现决定。
 * AFTER_COMMIT：业务事务提交后才发通知，避免业务回滚却已通知。
 */
@Component
public class NotificationListener {

    private final MessageProvider messageProvider;

    public NotificationListener(MessageProvider messageProvider) {
        this.messageProvider = messageProvider;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEventMessage message) {
        if (!(message.payload() instanceof Map<?, ?> payload)) {
            return;
        }
        switch (message.eventType()) {
            case "task.assigned" -> {
                Long assignee = asLong(payload.get("assigneeId"));
                if (assignee != null) {
                    messageProvider.send(assignee, "任务指派", "你被指派了任务 #" + payload.get("taskId"));
                }
            }
            case "comment.created" -> {
                if (payload.get("mention") instanceof List<?> mentions) {
                    for (Object m : mentions) {
                        Long uid = asLong(m);
                        if (uid != null) {
                            messageProvider.send(uid, "评论提醒", "有人在评论中 @了你");
                        }
                    }
                }
            }
            case "approval.approved" -> notifyApplicant(payload, "立项审批", "你的立项审批已全部通过");
            case "approval.rejected" -> notifyApplicant(payload, "立项审批", "你的立项审批被驳回");
            default -> {
                // 其余事件暂不通知（审批人通知需事件携带审批人 ID，后续增强）
            }
        }
    }

    private void notifyApplicant(Map<?, ?> payload, String title, String content) {
        Long applicant = asLong(payload.get("applicantId"));
        if (applicant != null) {
            messageProvider.send(applicant, title, content);
        }
    }

    private Long asLong(Object v) {
        return v instanceof Number n ? n.longValue() : null;
    }
}
