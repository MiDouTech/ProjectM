package com.mido.pm.task.dto;

import com.mido.pm.task.domain.WorkHourCalc;

import java.math.BigDecimal;

/**
 * 工时汇总（任务级/项目级口径统一）：预估/实际/进度(%)/剩余。
 */
public record WorkHourSummaryVO(
        BigDecimal estHours,
        BigDecimal actualHours,
        BigDecimal progress,
        BigDecimal remainingHours) {

    /** 由预估/实际汇总值构造，统一套用 {@link WorkHourCalc} 口径。 */
    public static WorkHourSummaryVO of(BigDecimal est, BigDecimal actual) {
        return new WorkHourSummaryVO(est, actual,
                WorkHourCalc.progressPercent(est, actual), WorkHourCalc.remaining(est, actual));
    }
}
