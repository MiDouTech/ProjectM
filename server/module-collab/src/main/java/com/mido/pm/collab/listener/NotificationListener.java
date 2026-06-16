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
 * 业务层不直接调通知。注入全部 MessageProvider 并扇出：P2 新增 WecomMessageProvider 即自动多通道，
 * 监听器零改动（接口抽象、加企微只加实现）。
 * AFTER_COMMIT：业务事务提交后才发通知，避免业务回滚却已通知。
 */
@Component
public class NotificationListener {

    private final List<MessageProvider> messageProviders;

    public NotificationListener(List<MessageProvider> messageProviders) {
        this.messageProviders = messageProviders;
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
                    notify(assignee, "任务指派", "你被指派了任务 #" + payload.get("taskId"));
                }
            }
            case "comment.created" -> {
                if (payload.get("mention") instanceof List<?> mentions) {
                    for (Object m : mentions) {
                        Long uid = asLong(m);
                        if (uid != null) {
                            notify(uid, "评论提醒", "有人在评论中 @了你");
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
            notify(applicant, title, content);
        }
    }

    /** 扇出到全部已激活通道（inapp 现行；wecom P2 加实现即自动生效）。 */
    private void notify(Long userId, String title, String content) {
        for (MessageProvider provider : messageProviders) {
            provider.send(userId, title, content);
        }
    }

    private Long asLong(Object v) {
        return v instanceof Number n ? n.longValue() : null;
    }
}
