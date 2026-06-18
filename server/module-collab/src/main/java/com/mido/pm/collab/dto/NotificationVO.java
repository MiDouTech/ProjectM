package com.mido.pm.collab.dto;

import java.time.LocalDateTime;

public record NotificationVO(
        Long id,
        String type,
        String bizType,
        Long bizId,
        String title,
        String payload,
        String link,
        Integer isRead,
        String channel,
        LocalDateTime createTime) {
}
