package com.mido.pm.provider.message;

/**
 * 企微消息 Provider（TODO·P2 激活）：企微应用消息推送。
 * 阶段一不注册为 Bean，占位预留。
 */
public class WecomMessageProvider implements MessageProvider {

    @Override
    public void send(Long userId, String title, String content) {
        throw new UnsupportedOperationException("TODO: 企微应用消息推送，P2 激活");
    }
}
