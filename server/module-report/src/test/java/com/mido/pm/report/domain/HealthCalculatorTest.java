package com.mido.pm.report.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 项目健康度综合（进度/预算/逾期）边界单测。 */
class HealthCalculatorTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void healthyGreen() {
        assertEquals(HealthLevel.GREEN, HealthCalculator.evaluate(bd("80"), bd("5"), bd("50")));
    }

    @Test
    void redWhenOverdueHigh() {
        assertEquals(HealthLevel.RED, HealthCalculator.evaluate(bd("80"), bd("35"), bd("50")));
    }

    @Test
    void redWhenOverBudget() {
        assertEquals(HealthLevel.RED, HealthCalculator.evaluate(bd("80"), bd("5"), bd("120")));
    }

    @Test
    void yellowWhenOverdueModerate() {
        assertEquals(HealthLevel.YELLOW, HealthCalculator.evaluate(bd("80"), bd("15"), bd("50")));
    }

    @Test
    void yellowWhenLowCompletion() {
        assertEquals(HealthLevel.YELLOW, HealthCalculator.evaluate(bd("40"), bd("5"), bd("50")));
    }

    @Test
    void boundaryOverdue30IsYellowNotRed() {
        // 30 不触发 red(>30)，但触发 yellow(>10)
        assertEquals(HealthLevel.YELLOW, HealthCalculator.evaluate(bd("80"), bd("30"), bd("50")));
    }

    @Test
    void nullBudgetTreatedAsZero() {
        assertEquals(HealthLevel.GREEN, HealthCalculator.evaluate(bd("80"), bd("5"), null));
    }
}
