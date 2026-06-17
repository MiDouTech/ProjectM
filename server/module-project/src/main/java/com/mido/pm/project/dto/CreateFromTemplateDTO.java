package com.mido.pm.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 按模板建项目入参。subCategory 可覆盖模板默认。 */
public record CreateFromTemplateDTO(
        @NotNull(message = "模板不能为空") Long templateId,
        @NotBlank(message = "项目名称不能为空") String name,
        Long leaderId,
        BigDecimal budget,
        String subCategory,
        LocalDate startDate,
        LocalDate endDate) {
}
