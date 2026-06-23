package com.mido.pm.collab;

import com.mido.pm.collab.entity.PmNotification;
import com.mido.pm.collab.listener.NotificationListener;
import com.mido.pm.collab.mapper.PmNotificationMapper;
import com.mido.pm.collab.provider.InAppMessageProvider;
import com.mido.pm.common.outbox.DomainEventMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 链路测试：指派任务 → 被指派人收到站内信。
 * 覆盖 collab 侧链路：task.assigned 事件 → NotificationListener → InAppMessageProvider → pm_notification(inapp)。
 * （TaskService.assign 发 task.assigned 事件已在 task 模块单测覆盖，二者合成完整链路。）
 */
@ExtendWith(MockitoExtension.class)
class NotificationLinkTest {

    @Mock
    private PmNotificationMapper notificationMapper;

    private NotificationListener listener() {
        return new NotificationListener(List.of(new InAppMessageProvider(notificationMapper)));
    }

    @Test
    void taskAssignedNotifiesAssigneeViaInApp() {
        listener().onDomainEvent(new DomainEventMessage(
                "task.assigned", Map.of("taskId", 55L, "assigneeId", 100L), 1L));

        ArgumentCaptor<PmNotification> captor = ArgumentCaptor.forClass(PmNotification.class);
        verify(notificationMapper).insert(captor.capture());
        PmNotification n = captor.getValue();
        assertEquals(100L, n.getUserId(), "被指派人应收到通知");
        assertEquals("inapp", n.getChannel());
        assertEquals(0, n.getIsRead());
    }

    @Test
    void commentMentionNotifiesEachMentioned() {
        listener().onDomainEvent(new DomainEventMessage(
                "comment.created",
                Map.of("commentId", 1L, "entityType", "task", "entityId", 9L,
                        "userId", 7L, "mention", List.of(100L, 200L)), 1L));

        verify(notificationMapper, times(2)).insert(any(PmNotification.class));
    }

    @Test
    void approvalSubmittedNotifiesApproversAndCc() {
        listener().onDomainEvent(new DomainEventMessage(
                "approval.submitted",
                Map.of("instanceId", 1L, "approverIds", List.of(100L), "ccIds", List.of(200L, 300L)), 1L));

        // 1 审批人 + 2 知会人 各一条站内信
        verify(notificationMapper, times(3)).insert(any(PmNotification.class));
    }

    @Test
    void briefingSubmittedNotifiesReviewers() {
        listener().onDomainEvent(new DomainEventMessage(
                "briefing.submitted",
                Map.of("briefingId", 9L, "authorId", 100L, "reviewerIds", List.of(300L)), 1L));

        ArgumentCaptor<PmNotification> captor = ArgumentCaptor.forClass(PmNotification.class);
        verify(notificationMapper).insert(captor.capture());
        assertEquals(300L, captor.getValue().getUserId(), "评审人应收到待评审通知");
    }

    @Test
    void briefingIssueRaisedNotifiesOwner() {
        listener().onDomainEvent(new DomainEventMessage(
                "briefing.issue.raised",
                Map.of("issueId", 1L, "briefingId", 9L, "ownerId", 500L, "raisedBy", 100L), 1L));

        ArgumentCaptor<PmNotification> captor = ArgumentCaptor.forClass(PmNotification.class);
        verify(notificationMapper).insert(captor.capture());
        assertEquals(500L, captor.getValue().getUserId(), "负责人应收到跟进问题通知");
    }
}
