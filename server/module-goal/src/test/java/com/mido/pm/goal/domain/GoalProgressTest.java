package com.mido.pm.goal.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * KR 进度计算单测（含 target==start 边界、越界钳制、递减型指标、空值）。
 */
class GoalProgressTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void normalAscending() {
        assertEquals(bd("50.00"), GoalProgress.compute(bd("0"), bd("100"), bd("50")));
    }

    @Test
    void targetEqualsStartReturnsZeroNoDivByZero() {
        assertEquals(bd("0.00"), GoalProgress.compute(bd("100"), bd("100"), bd("100")));
    }

    @Test
    void overachieveClampedTo100() {
        assertEquals(bd("100.00"), GoalProgress.compute(bd("0"), bd("100"), bd("130")));
    }

    @Test
    void belowStartClampedTo0() {
        assertEquals(bd("0.00"), GoalProgress.compute(bd("10"), bd("100"), bd("5")));
    }

    @Test
    void descendingMetric() {
        // 降低指标：start=100 → target=0，current=75 → 完成 25%
        assertEquals(bd("25.00"), GoalProgress.compute(bd("100"), bd("0"), bd("75")));
    }

    @Test
    void nullMetricReturnsZero() {
        assertEquals(bd("0.00"), GoalProgress.compute(null, bd("100"), bd("50")));
        assertEquals(bd("0.00"), GoalProgress.compute(bd("0"), bd("100"), null));
    }
}
