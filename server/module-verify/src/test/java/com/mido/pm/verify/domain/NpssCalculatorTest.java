package com.mido.pm.verify.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mido.pm.verify.domain.NpssCalculator.ScoreWeight;

/**
 * NPSS 加权算分与三档边界（npss-rule §9.1/§9.2）。
 */
class NpssCalculatorTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static ScoreWeight sw(int score, String weight) {
        return new ScoreWeight(score, bd(weight));
    }

    @Test
    void weightedNormalizesPercentWeights() {
        // 权重以百分比存(和=100)，计算前归一化：(Σsw/Σw)×10
        // 9×30+10×30+8×10+9×10+9×20 = 920；/100 = 9.2；×10 = 92.00
        BigDecimal w = NpssCalculator.weightedSatisfaction(List.of(
                sw(9, "30"), sw(10, "30"), sw(8, "10"), sw(9, "10"), sw(9, "20")));
        assertEquals(0, bd("92.00").compareTo(w));
    }

    @Test
    void weightedNormalizesWhenSumNot100() {
        // 权重和≠1 也归一化：单人 score 9 → 90
        assertEquals(0, bd("90.00").compareTo(
                NpssCalculator.weightedSatisfaction(List.of(sw(9, "0.5")))));
    }

    @Test
    void levelBoundary90() {
        assertEquals(ResultLevel.SUCCESS, NpssCalculator.level(bd("90")));
        // 9×0.9 + 8×0.1 = 8.9 → 89 → mixed（90 临界下方）
        BigDecimal w = NpssCalculator.weightedSatisfaction(List.of(sw(9, "0.9"), sw(8, "0.1")));
        assertEquals(0, bd("89.00").compareTo(w));
        assertEquals(ResultLevel.MIXED, NpssCalculator.level(w));
    }

    @Test
    void levelBoundary70() {
        assertEquals(ResultLevel.MIXED, NpssCalculator.level(bd("70")));
        // 7×0.9 + 6×0.1 = 6.9 → 69 → failure（70 临界下方）
        BigDecimal w = NpssCalculator.weightedSatisfaction(List.of(sw(7, "0.9"), sw(6, "0.1")));
        assertEquals(0, bd("69.00").compareTo(w));
        assertEquals(ResultLevel.FAILURE, NpssCalculator.level(w));
    }

    @Test
    void emptyScoresZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(NpssCalculator.weightedSatisfaction(List.of())));
    }
}
