package com.mido.pm.provider.message;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 通道路由策略单测：站内信兜底，重要/时效强事件额外推企微。
 */
class MessageRoutingTest {

    @Test
    void actionableEventGoesInAppAndWecom() {
        assertEquals(Set.of(MessageProvider.CHANNEL_INAPP, MessageProvider.CHANNEL_WECOM),
                MessageRouting.channelsFor("task.assigned"));
        assertEquals(Set.of(MessageProvider.CHANNEL_INAPP, MessageProvider.CHANNEL_WECOM),
                MessageRouting.channelsFor("project.budget.exceeded"));
    }

    @Test
    void ordinaryEventInAppOnly() {
        assertEquals(Set.of(MessageProvider.CHANNEL_INAPP), MessageRouting.channelsFor("comment.created"));
    }

    @Test
    void unknownEventInAppOnly() {
        assertEquals(Set.of(MessageProvider.CHANNEL_INAPP), MessageRouting.channelsFor("something.else"));
    }

    @Test
    void nullEventInAppOnly() {
        assertEquals(Set.of(MessageProvider.CHANNEL_INAPP), MessageRouting.channelsFor(null));
    }
}
