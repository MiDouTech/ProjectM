package com.mido.pm.provider.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * 企微消息 Provider 单测：通道标识、开关门控（默认关→Mock 不外呼；开启+凭证→真发投递）。
 */
class WecomMessageProviderTest {

    @Test
    void channelIsWecom() {
        assertEquals(MessageProvider.CHANNEL_WECOM,
                new WecomMessageProvider(false, "", "", "").channel());
    }

    @Test
    void disabledGoesMockAndDoesNotDispatch() {
        WecomMessageProvider provider = spy(new WecomMessageProvider(false, "corp", "sec", "1000002"));
        provider.send(100L, "任务指派", "你被指派了任务 #55");
        // 默认关：仅 Mock 打印，不进入真实投递
        verify(provider, never()).dispatch(anyLong(), any(), any());
    }

    @Test
    void enabledWithCredentialsDispatches() {
        WecomMessageProvider provider = spy(new WecomMessageProvider(true, "corp", "sec", "1000002"));
        provider.send(100L, "任务指派", "你被指派了任务 #55");
        verify(provider).dispatch(100L, "任务指派", "你被指派了任务 #55");
    }

    @Test
    void enabledButNoCorpIdFallsBackToMock() {
        WecomMessageProvider provider = spy(new WecomMessageProvider(true, "", "sec", "1000002"));
        provider.send(100L, "标题", "正文");
        // 缺 corpId 视为未就绪，不外呼
        verify(provider, never()).dispatch(anyLong(), any(), any());
    }

    @Test
    void nullUserIsNoop() {
        WecomMessageProvider provider = spy(new WecomMessageProvider(true, "corp", "sec", "1000002"));
        provider.send(null, "t", "c");
        verify(provider, never()).dispatch(anyLong(), any(), any());
    }
}
