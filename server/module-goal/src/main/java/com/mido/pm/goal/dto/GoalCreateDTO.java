package com.mido.pm.goal.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/** 新建目标/KR。type=objective|kr；parentId 为空=顶层目标；量化指标可空。 */
public record GoalCreateDTO(
        @NotBlank(message = "标题不能为空") String title,
        @NotBlank(message = "类型不能为空") String type,
        Long parentId,
        Long ownerId,
        String period,
        String metricUnit,
        BigDecimal metricStart,
        BigDecimal metricTarget,
        BigDecimal metricCurrent,
        Integer autoRollup) {
}
