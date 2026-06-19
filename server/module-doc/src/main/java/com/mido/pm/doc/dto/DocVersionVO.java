package com.mido.pm.doc.dto;

import java.time.LocalDateTime;

/** 文档版本视图。content 仅在取单个版本时返回，列表时为 null。 */
public record DocVersionVO(
        Long id,
        Long docId,
        Integer versionNo,
        String title,
        String content,
        String changeNote,
        Long createBy,
        LocalDateTime createTime) {
}
