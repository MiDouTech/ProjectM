package com.mido.pm.doc.dto;

import java.time.LocalDateTime;

/** 回收站条目（已移入回收站、未彻底删除）。 */
public record DocTrashVO(
        Long id,
        String type,
        String title,
        LocalDateTime trashedTime) {
}
