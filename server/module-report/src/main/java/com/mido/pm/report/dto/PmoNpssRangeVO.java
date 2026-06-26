package com.mido.pm.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PMO 总体评价（任意周期，npss-rule §5）：成功%−失败%，对比基线 36。
 * from/to 为统计区间 [from, to)（按 reviewed_at）；动态计算组织在一定周期内的 NPSS。
 */
public record PmoNpssRangeVO(
        LocalDate from,
        LocalDate to,
        long total,
        long success,
        long mixed,
        long failure,
        BigDecimal successRate,
        BigDecimal failureRate,
        BigDecimal pmoNpss,
        BigDecimal baseline,
        boolean aboveBaseline) {
}
