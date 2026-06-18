package com.mido.pm.report.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * PMO 总体评价（npss-rule §5，纯函数）：PMO NPSS = 成功项目占比% − 失败项目占比%。
 * 全球基线 36（48% 成功 − 12% 失败）；米多目标 &gt; 36。
 */
public final class PmoNpssCalculator {

    /** 全球基线 */
    public static final BigDecimal BASELINE = BigDecimal.valueOf(36);

    /** @param total 总数；successRate/failureRate 百分比；pmoNpss = 成功% − 失败%。 */
    public record Stats(long total, BigDecimal successRate, BigDecimal failureRate, BigDecimal pmoNpss) {
    }

    private PmoNpssCalculator() {
    }

    public static Stats compute(long success, long mixed, long failure) {
        long total = success + mixed + failure;
        if (total == 0) {
            return new Stats(0, zero(), zero(), zero());
        }
        BigDecimal successRate = rate(success, total);
        BigDecimal failureRate = rate(failure, total);
        return new Stats(total, successRate, failureRate,
                successRate.subtract(failureRate).setScale(2, RoundingMode.HALF_UP));
    }

    private static BigDecimal rate(long count, long total) {
        return BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
