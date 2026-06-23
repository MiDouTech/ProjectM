package com.mido.pm.briefing.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/** 简报实例视图。 */
public record BriefingVO(
        Long id,
        Long templateId,
        String type,
        Long authorId,
        String periodKey,
        LocalDate periodStart,
        LocalDate periodEnd,
        Map<String, Object> content,
        String status,
        LocalDateTime submittedAt) {
}
