package com.mido.pm.collab.dto;

import java.time.LocalDateTime;

public record NotificationVO(
        Long id,
        String type,
        String title,
        String payload,
        Integer isRead,
        String channel,
        LocalDateTime createTime) {
}
