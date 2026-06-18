package com.mido.pm.collab.provider;

import cn.hutool.json.JSONUtil;
import com.mido.pm.collab.entity.PmNotification;
import com.mido.pm.collab.mapper.PmNotificationMapper;
import com.mido.pm.provider.message.MessageProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 站内信 MessageProvider 本地实现：写 pm_notification(channel=inapp)。
 * 因写业务表 pm_notification，置于 collab 模块（provider 不依赖业务模块）；企微实现 {@code WecomMessageProvider} 预留。
 */
@Component
public class InAppMessageProvider implements MessageProvider {

    private static final String TYPE_SYSTEM = "system";

    private final PmNotificationMapper notificationMapper;

    public InAppMessageProvider(PmNotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public String channel() {
        return CHANNEL_INAPP;
    }

    @Override
    public void send(Long userId, String title, String content) {
        if (userId == null) {
            return;
        }
        PmNotification n = new PmNotification();
        n.setUserId(userId);
        n.setType(TYPE_SYSTEM);
        n.setTitle(title);
        n.setPayload(JSONUtil.toJsonStr(Map.of("content", content == null ? "" : content)));
        n.setIsRead(0);
        n.setChannel(PmNotification.CHANNEL_INAPP);
        notificationMapper.insert(n);
    }
}
