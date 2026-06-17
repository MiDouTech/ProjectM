package com.mido.pm.report.dto;

import java.math.BigDecimal;

/** 项目健康度（进度/预算/逾期综合）。budgetUsage 为空表示未设预算。 */
public record ProjectHealthVO(
        Long projectId,
        long taskTotal,
        BigDecimal completionRate,
        BigDecimal overdueRate,
        BigDecimal budgetUsage,
        String health,
        String healthLabel) {
}
