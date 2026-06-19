package com.mido.pm.goal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** 调整对齐贡献权重。 */
public record AlignmentWeightDTO(
        @NotNull(message = "权重不能为空") @PositiveOrZero(message = "权重需为非负数") BigDecimal weight) {
}
