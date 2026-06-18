package com.mido.pm.provider.message;

import java.util.Set;

/**
 * 消息通道路由策略（纯函数，可单测）：按领域事件类型决定投递到哪些通道。
 *
 * <p>原则：站内信兜底——凡需通知的事件都进站内信；其中「可执行 / 时效强 / 预警类」事件
 * 额外推企微应用消息，以便用户离开系统也能第一时间收到。事件名取自 {@code docs/domain-events.md}。</p>
 */
public final class MessageRouting {

    /** 除站内信外，还需推企微的事件（可执行 / 时效强 / 预警）。 */
    private static final Set<String> ALSO_WECOM = Set.of(
            "task.assigned",            // 被指派任务，需立即知晓
            "approval.submitted",       // 待审批人尽快处理
            "approval.approved",        // 申请人关心结果
            "approval.rejected",        // 申请人需重新提交
            "npss.review.started",      // 干系人需去打分
            "project.budget.exceeded",  // 预算预警
            "cost.exceeded.budget");    // 费用超预算预警

    private MessageRouting() {
    }

    /**
     * 该事件应投递的通道集合。
     * 默认仅站内信；命中 {@link #ALSO_WECOM} 则同时推企微。
     */
    public static Set<String> channelsFor(String eventType) {
        if (eventType != null && ALSO_WECOM.contains(eventType)) {
            return Set.of(MessageProvider.CHANNEL_INAPP, MessageProvider.CHANNEL_WECOM);
        }
        return Set.of(MessageProvider.CHANNEL_INAPP);
    }
}
