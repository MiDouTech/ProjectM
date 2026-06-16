package com.mido.pm.project.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectCreateDTO(
        @NotBlank(message = "项目名称不能为空") String name,
        @NotBlank(message = "项目类型不能为空") String category,
        String subCategory,
        Long leaderId,
        BigDecimal budget,
        Long templateId,
        String description,
        LocalDate startDate,
        LocalDate endDate) {
}
