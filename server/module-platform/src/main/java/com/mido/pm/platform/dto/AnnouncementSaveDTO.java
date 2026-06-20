package com.mido.pm.platform.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/** 公告保存入参。status：draft/published；level：info/warning。 */
public record AnnouncementSaveDTO(
        @NotBlank(message = "标题不能为空") String title,
        @NotBlank(message = "内容不能为空") String content,
        String level,
        String status,
        LocalDateTime publishAt,
        LocalDateTime expireAt) {
}
