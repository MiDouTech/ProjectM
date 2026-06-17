package com.mido.pm.verify.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 奖金硬校验（npss-rule §9.5/§9.6）：归零线 60、无奖金类型。
 */
class BonusCalculatorTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void satisfactionBelow60ZerosBonus() {
        // 59 → 归零
        assertEquals(0, BigDecimal.ZERO.compareTo(
                BonusCalculator.compute("S", null, bd("1000"), bd("1"), bd("59"))));
    }

    @Test
    void satisfaction60Normal() {
        // 60 → 1000 × 1 × 0.6 = 600
        assertEquals(0, bd("600.00").compareTo(
                BonusCalculator.compute("S", null, bd("1000"), bd("1"), bd("60"))));
    }

    @Test
    void normalTypeComputes() {
        // S 类 满意度90：1000 × 1 × 0.9 = 900
        assertEquals(0, bd("900.00").compareTo(
                BonusCalculator.compute("S", "常规", bd("1000"), bd("1"), bd("90"))));
    }

    @Test
    void oRectifyNoBonus() {
        // O·定向整改 → 0（即便满意度很高，也不报错）
        assertEquals(0, BigDecimal.ZERO.compareTo(
                BonusCalculator.compute("O", "定向整改", bd("1000"), bd("1"), bd("95"))));
    }

    @Test
    void oSuperviseNoBonus() {
        // O·专项督办 → 0
        assertEquals(0, BigDecimal.ZERO.compareTo(
                BonusCalculator.compute("O", "专项督办", bd("1000"), bd("1"), bd("95"))));
    }
}
