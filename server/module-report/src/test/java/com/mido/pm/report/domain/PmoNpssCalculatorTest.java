package com.mido.pm.report.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PMO 总体评价单测（npss-rule §5）：成功%−失败%，对比基线 36；空集不除零。
 */
class PmoNpssCalculatorTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void docExample60Success10Failure30Mixed() {
        // §5 例：100 项目 60 成功 / 10 失败 / 30 混合 → PMO NPSS = 60 − 10 = 50
        PmoNpssCalculator.Stats s = PmoNpssCalculator.compute(60, 30, 10);
        assertEquals(100, s.total());
        assertEquals(0, bd("60.00").compareTo(s.successRate()));
        assertEquals(0, bd("10.00").compareTo(s.failureRate()));
        assertEquals(0, bd("50.00").compareTo(s.pmoNpss()));
    }

    @Test
    void emptyNoDivByZero() {
        PmoNpssCalculator.Stats s = PmoNpssCalculator.compute(0, 0, 0);
        assertEquals(0, s.total());
        assertEquals(0, BigDecimal.ZERO.compareTo(s.pmoNpss()));
    }

    @Test
    void baselineIs36() {
        assertEquals(0, bd("36").compareTo(PmoNpssCalculator.BASELINE));
    }
}
