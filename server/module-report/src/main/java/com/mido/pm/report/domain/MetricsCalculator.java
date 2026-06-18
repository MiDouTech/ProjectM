package com.mido.pm.report.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 度量比率（纯函数）：占比% = part/total×100，total=0 → 0（不除零）。 */
public final class MetricsCalculator {

    private MetricsCalculator() {
    }

    /** 百分比（0-100，2 位）。 */
    public static BigDecimal rate(long part, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(part).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
