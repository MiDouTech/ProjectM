package com.mido.pm.goal.dto;

import java.math.BigDecimal;

/** 目标/KR 视图。progress 为计算值（0-100）。 */
public record GoalVO(
        Long id,
        String title,
        String type,
        Long parentId,
        Long ownerId,
        String period,
        String metricUnit,
        BigDecimal metricStart,
        BigDecimal metricTarget,
        BigDecimal metricCurrent,
        BigDecimal progress) {
}
