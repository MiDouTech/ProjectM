package com.mido.pm.provider.message;

/**
 * 消息推送 Provider 接口。屏蔽底层通道（站内信 / 企微应用消息）。
 * 每个实现声明自己的 {@link #channel()}，由 {@code NotificationListener} 结合
 * {@link MessageRouting} 的「事件类型 → 通道」策略决定是否投递到本通道。
 */
public interface MessageProvider {

    /** 站内信通道 */
    String CHANNEL_INAPP = "inapp";
    /** 企微应用消息通道 */
    String CHANNEL_WECOM = "wecom";

    /** 本实现所属通道（用于事件路由匹配）。 */
    String channel();

    /**
     * 向用户推送一条消息。
     *
     * @param userId  接收人
     * @param title   标题
     * @param content 正文
     */
    void send(Long userId, String title, String content);
}
