package com.mido.pm.collab.listener;

import com.mido.pm.common.outbox.DomainEventMessage;
import com.mido.pm.provider.message.MessageProvider;
import com.mido.pm.provider.message.MessageRouting;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通知监听器（事件驱动）：订阅领域事件 → 解析收件人 → 经 {@link MessageProvider} 发通知。
 * 业务层不直接调通知。注入全部 MessageProvider，按 {@link MessageRouting} 的「事件类型 → 通道」
 * 策略路由：站内信兜底，重要/时效强事件额外推企微。新增通道只加实现 + 路由表，监听器逻辑不变。
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
        String eventType = message.eventType();
        switch (eventType) {
            case "task.assigned" -> {
                Long assignee = asLong(payload.get("assigneeId"));
                if (assignee != null) {
                    notify(eventType, assignee, "任务指派", "你被指派了任务 #" + payload.get("taskId"));
                }
            }
            case "comment.created" -> {
                if (payload.get("mention") instanceof List<?> mentions) {
                    for (Object m : mentions) {
                        Long uid = asLong(m);
                        if (uid != null) {
                            notify(eventType, uid, "评论提醒", "有人在评论中 @了你");
                        }
                    }
                }
            }
            case "approval.approved" -> notifyApplicant(eventType, payload, "立项审批", "你的立项审批已全部通过");
            case "approval.rejected" -> notifyApplicant(eventType, payload, "立项审批", "你的立项审批被驳回");
            default -> {
                // 其余事件暂不通知（审批人通知需事件携带审批人 ID，后续增强）
            }
        }
    }

    private void notifyApplicant(String eventType, Map<?, ?> payload, String title, String content) {
        Long applicant = asLong(payload.get("applicantId"));
        if (applicant != null) {
            notify(eventType, applicant, title, content);
        }
    }

    /** 按事件路由扇出：只投递到 {@link MessageRouting#channelsFor} 命中的通道。 */
    private void notify(String eventType, Long userId, String title, String content) {
        Set<String> channels = MessageRouting.channelsFor(eventType);
        for (MessageProvider provider : messageProviders) {
            if (channels.contains(provider.channel())) {
                provider.send(userId, title, content);
            }
        }
    }

    private Long asLong(Object v) {
        return v instanceof Number n ? n.longValue() : null;
    }
}
