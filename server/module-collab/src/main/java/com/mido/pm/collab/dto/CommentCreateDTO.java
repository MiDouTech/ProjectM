package com.mido.pm.collab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 评论。entityType：task/project/goal；mention 为 @用户 ID 列表。 */
public record CommentCreateDTO(
        @NotBlank(message = "评论对象类型不能为空") String entityType,
        @NotNull(message = "评论对象不能为空") Long entityId,
        @NotBlank(message = "评论内容不能为空") String content,
        List<Long> mention) {
}
