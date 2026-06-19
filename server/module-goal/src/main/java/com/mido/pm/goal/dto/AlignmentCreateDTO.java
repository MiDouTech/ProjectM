package com.mido.pm.goal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** 新增对齐：目标弱关联到 project/task。weight 为贡献权重（可空，默认 1）。 */
public record AlignmentCreateDTO(
        @NotBlank(message = "对齐目标类型不能为空") String targetType,
        @NotNull(message = "对齐目标不能为空") Long targetId,
        BigDecimal weight) {
}
