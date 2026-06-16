package com.mido.pm.provider.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 本地消息 Provider 占位实现。阶段一写站内信（pm_notification），企微推送实现预留。
 */
@Component
public class LocalMessageProvider implements MessageProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalMessageProvider.class);

    @Override
    public void send(Long userId, String title, String content) {
        // TODO 阶段二写入 pm_notification（channel=inapp）
        log.info("[站内信占位] to={}, title={}, content={}", userId, title, content);
    }
}
