package com.mido.pm.collab;

import com.mido.pm.collab.entity.PmNotification;
import com.mido.pm.collab.listener.NotificationListener;
import com.mido.pm.collab.mapper.PmNotificationMapper;
import com.mido.pm.collab.provider.InAppMessageProvider;
import com.mido.pm.common.outbox.DomainEventMessage;
import com.mido.pm.provider.message.WecomMessageProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 链路测试：任务指派事件 → 多通道路由 → Mock 企微推送。
 * task.assigned 命中 {@code MessageRouting} 的「站内信 + 企微」：站内信落 pm_notification，
 * 企微走 WecomMessageProvider（开关默认关 → Mock 预演，不外呼）。
 * 同时验证 comment.created 仅站内信、不推企微（路由生效的反例）。
 */
@ExtendWith(MockitoExtension.class)
class WecomNotificationLinkTest {

    @Mock
    private PmNotificationMapper notificationMapper;

    private NotificationListener listenerWith(WecomMessageProvider wecom) {
        return new NotificationListener(List.of(new InAppMessageProvider(notificationMapper), wecom));
    }

    @Test
    void taskAssignedAlsoPushesWecomMock() {
        WecomMessageProvider wecom = spy(new WecomMessageProvider(false, "", "", "")); // 默认关：Mock 预演

        listenerWith(wecom).onDomainEvent(new DomainEventMessage(
                "task.assigned", Map.of("taskId", 55L, "assigneeId", 100L), 1L));

        // 站内信：落库
        verify(notificationMapper).insert(any(PmNotification.class));
        // 企微：被指派人收到（Mock 预演推送，不外呼）
        verify(wecom).send(100L, "任务指派", "你被指派了任务 #55");
    }

    @Test
    void npssReviewStartedNotifiesEachRecipientBothChannels() {
        WecomMessageProvider wecom = spy(new WecomMessageProvider(false, "", "", ""));

        listenerWith(wecom).onDomainEvent(new DomainEventMessage(
                "npss.review.started",
                Map.of("reviewId", 3L, "projectId", 100L, "recipientUserIds", List.of(8L, 9L)), 1L));

        // 两名干系人：站内信各一条 + 企微各一条（npss.review.started 命中 inapp+wecom）
        verify(notificationMapper, times(2)).insert(any(PmNotification.class));
        verify(wecom).send(8L, "价值验收待打分", "项目价值验收已发起，请为项目交付价值打分（0-10）。");
        verify(wecom).send(9L, "价值验收待打分", "项目价值验收已发起，请为项目交付价值打分（0-10）。");
    }

    @Test
    void budgetExceededNotifiesLeaderBothChannels() {
        WecomMessageProvider wecom = spy(new WecomMessageProvider(false, "", "", ""));

        listenerWith(wecom).onDomainEvent(new DomainEventMessage(
                "project.budget.exceeded",
                Map.of("projectId", 42L, "leaderId", 7L, "budget", 1000, "cumulativeActual", 1200), 1L));

        verify(notificationMapper).insert(any(PmNotification.class));
        verify(wecom).send(7L, "预算预警", "项目 #42 实际成本已超预算，请关注。");
    }

    @Test
    void approvalSubmittedNotifiesEachApproverBothChannels() {
        WecomMessageProvider wecom = spy(new WecomMessageProvider(false, "", "", ""));

        listenerWith(wecom).onDomainEvent(new DomainEventMessage(
                "approval.submitted",
                Map.of("instanceId", 5L, "applicantId", 7L, "approverIds", List.of(100L, 200L)), 1L));

        verify(notificationMapper, times(2)).insert(any(PmNotification.class));
        verify(wecom).send(100L, "待审批", "有一条立项审批待你处理。");
        verify(wecom).send(200L, "待审批", "有一条立项审批待你处理。");
    }

    @Test
    void commentMentionStaysInAppOnly() {
        WecomMessageProvider wecom = spy(new WecomMessageProvider(false, "", "", ""));

        listenerWith(wecom).onDomainEvent(new DomainEventMessage(
                "comment.created", Map.of("mention", List.of(100L)), 1L));

        verify(notificationMapper).insert(any(PmNotification.class));
        verify(wecom, never()).send(anyLong(), any(), any());
    }
}
