package com.mido.pm.provider.message;

/**
 * 消息推送 Provider 接口。屏蔽底层通道（站内信 / 企微应用消息）。
 */
public interface MessageProvider {

    /**
     * 向用户推送一条消息。
     *
     * @param userId  接收人
     * @param title   标题
     * @param content 正文
     */
    void send(Long userId, String title, String content);
}
