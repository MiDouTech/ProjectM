package com.mido.pm.report.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 完成率/逾期率口径单测：占比%，total=0 不除零。 */
class MetricsCalculatorTest {

    @Test
    void ratePercent() {
        assertEquals(0, new BigDecimal("75.00").compareTo(MetricsCalculator.rate(3, 4)));
    }

    @Test
    void zeroTotalNoDivByZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(MetricsCalculator.rate(0, 0)));
    }
}
