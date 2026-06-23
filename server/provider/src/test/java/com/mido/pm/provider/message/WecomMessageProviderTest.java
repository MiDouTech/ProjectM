package com.mido.pm.provider.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 企微消息 Provider 单测：通道标识、开关门控（默认关→Mock 不外呼；开启+凭证→真发投递）、
 * dispatch 路由（解析到企微 userid→push；未绑定→跳过）。
 */
class WecomMessageProviderTest {

    private final WecomUserResolver resolver = mock(WecomUserResolver.class);
    private final WecomMessageClient client = mock(WecomMessageClient.class);

    private WecomMessageProvider provider(boolean enabled, String corp, String secret, String agent) {
        return new WecomMessageProvider(enabled, corp, secret, agent, resolver, client);
    }

    @Test
    void channelIsWecom() {
        assertEquals(MessageProvider.CHANNEL_WECOM, provider(false, "", "", "").channel());
    }

    @Test
    void disabledGoesMockAndDoesNotDispatch() {
        WecomMessageProvider p = spy(provider(false, "corp", "sec", "1000002"));
        p.send(100L, "任务指派", "你被指派了任务 #55");
        verify(p, never()).dispatch(anyLong(), any(), any());
    }

    @Test
    void enabledWithCredentialsDispatches() {
        WecomMessageProvider p = spy(provider(true, "corp", "sec", "1000002"));
        p.send(100L, "任务指派", "你被指派了任务 #55");
        verify(p).dispatch(100L, "任务指派", "你被指派了任务 #55");
    }

    @Test
    void enabledButNoCorpIdFallsBackToMock() {
        WecomMessageProvider p = spy(provider(true, "", "sec", "1000002"));
        p.send(100L, "标题", "正文");
        verify(p, never()).dispatch(anyLong(), any(), any());
    }

    @Test
    void nullUserIsNoop() {
        WecomMessageProvider p = spy(provider(true, "corp", "sec", "1000002"));
        p.send(null, "t", "c");
        verify(p, never()).dispatch(anyLong(), any(), any());
    }

    @Test
    void dispatchPushesWhenUserBound() {
        when(resolver.externalUserId(100L)).thenReturn("wxuser1");
        provider(true, "corp", "sec", "1000002").send(100L, "任务指派", "正文");
        verify(client).push("corp", "sec", "1000002", "wxuser1", "任务指派", "正文");
    }

    @Test
    void dispatchSkipsWhenUserUnbound() {
        when(resolver.externalUserId(100L)).thenReturn(null);
        provider(true, "corp", "sec", "1000002").send(100L, "任务指派", "正文");
        verify(client, never()).push(any(), any(), any(), any(), any(), any());
    }
}
