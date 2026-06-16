package com.mido.pm.task.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskVO(
        Long id,
        Long projectId,
        Long parentId,
        String title,
        String description,
        Long assigneeId,
        String status,
        Integer priority,
        String stage,
        LocalDate startDate,
        LocalDate dueDate,
        Integer isMilestone,
        LocalDateTime createTime) {
}
