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
        return new NotificationListener(new InAppMessageProvider(notificationMapper));
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
}
