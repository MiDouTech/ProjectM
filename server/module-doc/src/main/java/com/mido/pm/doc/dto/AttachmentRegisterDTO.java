package com.mido.pm.doc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 上传登记（预签名直传）：服务端据此生成 oss_key 与预签名 PUT URL；entityType=project/task/cost。 */
public record AttachmentRegisterDTO(
        @NotBlank(message = "实体类型不能为空") String entityType,
        @NotNull(message = "实体ID不能为空") Long entityId,
        @NotBlank(message = "文件名不能为空") String name,
        @NotNull(message = "文件大小不能为空") @Positive(message = "文件大小须大于0") Long size,
        String contentType) {
}
