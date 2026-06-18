package com.mido.pm.task.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 工时汇总口径单测（先于实现）：进度=实际/预估，预估为 0 置 0 不报错；剩余=预估−实际可为负。
 */
class WorkHourCalcTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void progressIsActualOverEstPercent() {
        assertEquals(bd("75.0"), WorkHourCalc.progressPercent(bd("8"), bd("6")));
        assertEquals(bd("33.3"), WorkHourCalc.progressPercent(bd("3"), bd("1")));
    }

    @Test
    void progressZeroWhenEstimateZeroNoError() {
        assertEquals(bd("0.0"), WorkHourCalc.progressPercent(BigDecimal.ZERO, bd("5")));
        assertEquals(bd("0.0"), WorkHourCalc.progressPercent(null, bd("5")));
    }

    @Test
    void progressNotCappedAllowsOverrun() {
        assertEquals(bd("125.0"), WorkHourCalc.progressPercent(bd("4"), bd("5")));
    }

    @Test
    void remainingIsEstMinusActualMayBeNegative() {
        assertEquals(bd("2.00"), WorkHourCalc.remaining(bd("8"), bd("6")));
        assertEquals(bd("-1.00"), WorkHourCalc.remaining(bd("4"), bd("5")));
        assertEquals(bd("0.00"), WorkHourCalc.remaining(null, null));
    }
}
