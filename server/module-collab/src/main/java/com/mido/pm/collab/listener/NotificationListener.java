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
        // 审批类事件统一定位到审批实例（点击通知 → 打开该审批）
        Long instanceId = asLong(payload.get("instanceId"));
        String approvalLink = instanceId == null ? null : "/approval?open=" + instanceId;
        switch (eventType) {
            case "task.assigned" -> {
                Long assignee = asLong(payload.get("assigneeId"));
                if (assignee != null) {
                    Long taskId = asLong(payload.get("taskId"));
                    Long projectId = asLong(payload.get("projectId"));
                    String link = projectId == null || taskId == null ? null
                            : "/project/" + projectId + "/task/" + taskId;
                    notify(eventType, assignee, "任务指派", "你被指派了任务 #" + payload.get("taskId"),
                            "task", taskId, link);
                }
            }
            case "comment.created" -> {
                if (payload.get("mention") instanceof List<?> mentions) {
                    Long entityId = asLong(payload.get("entityId"));
                    String entityType = payload.get("entityType") == null ? null : String.valueOf(payload.get("entityType"));
                    for (Object m : mentions) {
                        Long uid = asLong(m);
                        if (uid != null) {
                            notify(eventType, uid, "评论提醒", "有人在评论中 @了你", entityType, entityId, null);
                        }
                    }
                }
            }
            case "approval.approved" -> notifyApplicant(eventType, payload, "立项审批", "你的立项审批已全部通过",
                    instanceId, approvalLink);
            case "approval.rejected" -> notifyApplicant(eventType, payload, "立项审批", "你的立项审批被驳回",
                    instanceId, approvalLink);
            case "approval.submitted" -> {
                notifyApprovers(eventType, payload.get("approverIds"), "待审批", "有一条立项审批待你处理。",
                        instanceId, approvalLink);
                notifyApprovers(eventType, payload.get("ccIds"), "审批知会", "有一条立项审批已发起，知会你知悉。",
                        instanceId, approvalLink);
            }
            case "approval.node.approved" -> {
                notifyApprovers(eventType, payload.get("nextApproverIds"), "待审批",
                        "上一节点已通过，有一条立项审批待你处理。", instanceId, approvalLink);
                notifyApprovers(eventType, payload.get("nextCcIds"), "审批知会", "审批已进入新节点，知会你知悉。",
                        instanceId, approvalLink);
            }
            case "approval.transferred" -> {
                Long toUser = asLong(payload.get("toUserId"));
                if (toUser != null) {
                    notify(eventType, toUser, "待审批", "有一条审批转交给你处理，请尽快查看。",
                            "approval", instanceId, approvalLink);
                }
            }
            case "approval.withdrawn" -> notifyApprovers(eventType, payload.get("approverIds"),
                    "审批撤回", "你待处理的一条立项审批已被发起人撤回，无需处理。", instanceId, approvalLink);
            case "npss.review.started" -> {
                // 收件干系人由事件携带（recipientUserIds），逐个多通道通知去打分
                Long projectId = asLong(payload.get("projectId"));
                String link = projectId == null ? null : "/project/" + projectId;
                if (payload.get("recipientUserIds") instanceof List<?> recipients) {
                    for (Object r : recipients) {
                        Long uid = asLong(r);
                        if (uid != null) {
                            notify(eventType, uid, "价值验收待打分",
                                    "项目价值验收已发起，请为项目交付价值打分（0-10）。", "project", projectId, link);
                        }
                    }
                }
            }
            case "project.budget.exceeded" -> {
                // 预算预警 → 通知项目负责人（仅此事件通知，避免与 cost.exceeded.budget 重复）
                Long leader = asLong(payload.get("leaderId"));
                if (leader != null) {
                    Long projectId = asLong(payload.get("projectId"));
                    String link = projectId == null ? null : "/project/" + projectId;
                    notify(eventType, leader, "预算预警",
                            "项目 #" + payload.get("projectId") + " 实际成本已超预算，请关注。",
                            "project", projectId, link);
                }
            }
            default -> {
                // 其余事件暂不通知（需要时按"事件携带收件人 ID"模式补分支）
            }
        }
    }

    private void notifyApplicant(String eventType, Map<?, ?> payload, String title, String content,
                                 Long bizId, String link) {
        Long applicant = asLong(payload.get("applicantId"));
        if (applicant != null) {
            notify(eventType, applicant, title, content, "approval", bizId, link);
        }
    }

    /** 通知一批审批人（事件携带的审批人 id 列表）。 */
    private void notifyApprovers(String eventType, Object approverIds, String title, String content,
                                 Long bizId, String link) {
        if (approverIds instanceof List<?> approvers) {
            for (Object a : approvers) {
                Long uid = asLong(a);
                if (uid != null) {
                    notify(eventType, uid, title, content, "approval", bizId, link);
                }
            }
        }
    }

    /** 按事件路由扇出：只投递到 {@link MessageRouting#channelsFor} 命中的通道；携带业务定位用于站内信跳转。 */
    private void notify(String eventType, Long userId, String title, String content,
                        String bizType, Long bizId, String link) {
        Set<String> channels = MessageRouting.channelsFor(eventType);
        for (MessageProvider provider : messageProviders) {
            if (channels.contains(provider.channel())) {
                provider.send(userId, title, content, bizType, bizId, link);
            }
        }
    }

    private Long asLong(Object v) {
        return v instanceof Number n ? n.longValue() : null;
    }
}
