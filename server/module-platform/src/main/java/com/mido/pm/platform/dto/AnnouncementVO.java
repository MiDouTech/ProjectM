package com.mido.pm.platform.dto;

import java.time.LocalDateTime;

/** 公告视图（运营管理与租户读取共用）。 */
public record AnnouncementVO(
        Long id,
        String title,
        String content,
        String level,
        String status,
        LocalDateTime publishAt,
        LocalDateTime expireAt,
        LocalDateTime createTime) {
}
