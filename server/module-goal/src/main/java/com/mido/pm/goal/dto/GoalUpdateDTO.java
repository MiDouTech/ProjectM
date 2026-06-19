package com.mido.pm.goal.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/** 编辑目标/KR。progress 由指标重算，不接受手填。 */
public record GoalUpdateDTO(
        @NotBlank(message = "标题不能为空") String title,
        Long ownerId,
        String period,
        String metricUnit,
        BigDecimal metricStart,
        BigDecimal metricTarget,
        BigDecimal metricCurrent,
        Integer autoRollup) {
}
