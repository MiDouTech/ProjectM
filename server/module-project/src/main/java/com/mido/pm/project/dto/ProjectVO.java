package com.mido.pm.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectVO(
        Long id,
        String code,
        String name,
        String description,
        String category,
        String subCategory,
        Long templateId,
        Long leaderId,
        String status,
        BigDecimal budget,
        BigDecimal actualCost,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate valueReviewDueDate,
        LocalDateTime pmoRegisteredAt,
        LocalDateTime createTime,
        Long deptId) {
}
