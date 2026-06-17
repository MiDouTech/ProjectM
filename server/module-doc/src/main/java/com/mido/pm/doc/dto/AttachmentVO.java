package com.mido.pm.doc.dto;

import java.time.LocalDateTime;

/**
 * 附件对外视图。**不含 oss_key**：下载经 {@code /attachments/{id}/download-url} 取限时预签名 URL。
 */
public record AttachmentVO(
        Long id,
        String entityType,
        Long entityId,
        String name,
        Long size,
        Long uploaderId,
        LocalDateTime createTime) {
}
