package com.mido.pm.task.dto;

import java.math.BigDecimal;

/** 人员级工时汇总（项目内按人分组），口径同 {@link WorkHourSummaryVO}。 */
public record PersonWorkHourSummaryVO(
        Long userId,
        BigDecimal estHours,
        BigDecimal actualHours,
        BigDecimal progress,
        BigDecimal remainingHours) {
}
