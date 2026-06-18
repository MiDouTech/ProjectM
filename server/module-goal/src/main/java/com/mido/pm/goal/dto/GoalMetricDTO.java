package com.mido.pm.goal.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** 量化指标行内编辑：仅更新当前值，进度自动重算。 */
public record GoalMetricDTO(
        @NotNull(message = "当前值不能为空") BigDecimal metricCurrent) {
}
