package com.mido.pm.stakeholder.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record StakeholderUpdateDTO(
        @NotBlank(message = "角色不能为空") String role,
        String category,
        String externalName,
        @Min(value = 1, message = "权力等级 1-5") @Max(value = 5, message = "权力等级 1-5") Integer powerLevel,
        @Min(value = 1, message = "利益等级 1-5") @Max(value = 5, message = "利益等级 1-5") Integer interestLevel,
        BigDecimal npssWeight) {
}
