package com.mido.pm.report.dto;

import java.math.BigDecimal;

/**
 * PMO 总体评价（按财年）。pmoNpss=成功%−失败%；baseline=全球基线 36；aboveBaseline 是否达标(>36)。
 */
public record PmoNpssVO(
        int fiscalYear,
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
