package com.mido.pm.collab.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentVO(
        Long id,
        String entityType,
        Long entityId,
        Long userId,
        String content,
        List<Long> mention,
        LocalDateTime createTime) {
}
