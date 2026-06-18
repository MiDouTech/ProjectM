package com.mido.pm.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectCreateDTO(
        @NotBlank(message = "项目名称不能为空") String name,
        @NotBlank(message = "项目类型不能为空") String category,
        String subCategory,
        @NotNull(message = "负责人不能为空") Long leaderId,
        BigDecimal budget,
        Long templateId,
        String description,
        LocalDate startDate,
        LocalDate endDate) {
}
