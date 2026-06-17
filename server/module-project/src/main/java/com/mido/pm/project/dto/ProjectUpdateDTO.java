package com.mido.pm.project.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectUpdateDTO(
        @NotBlank(message = "项目名称不能为空") String name,
        String subCategory,
        Long leaderId,
        BigDecimal budget,
        String description,
        LocalDate startDate,
        LocalDate endDate) {
}
