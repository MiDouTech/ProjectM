package com.mido.pm.verify.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * NPSS 单项目算分（npss-rule §3，纯函数）。
 * 加权满意度(0-100) = (Σ(score_i × weight_i) / Σweight_i) × 10（权重计算前归一化，允许以百分比存）。
 * 分级：≥90 success / 70..89 mixed / &lt;70 failure。
 */
public final class NpssCalculator {

    private static final BigDecimal TEN = BigDecimal.TEN;
    private static final BigDecimal NINETY = BigDecimal.valueOf(90);
    private static final BigDecimal SEVENTY = BigDecimal.valueOf(70);

    /** 一个干系人的打分与权重。 */
    public record ScoreWeight(int score, BigDecimal weight) {
    }

    private NpssCalculator() {
    }

    /** 加权满意度（0-100，2 位）；无评分或权重和为 0 → 0。 */
    public static BigDecimal weightedSatisfaction(List<ScoreWeight> scores) {
        if (scores == null || scores.isEmpty()) {
            return zero();
        }
        BigDecimal sumW = BigDecimal.ZERO;
        BigDecimal sumSW = BigDecimal.ZERO;
        for (ScoreWeight s : scores) {
            BigDecimal w = s.weight() == null ? BigDecimal.ZERO : s.weight();
            sumW = sumW.add(w);
            sumSW = sumSW.add(w.multiply(BigDecimal.valueOf(s.score())));
        }
        if (sumW.signum() == 0) {
            return zero();
        }
        // (Σsw / Σw) × 10 = 归一化加权均分 × 10
        return sumSW.divide(sumW, 6, RoundingMode.HALF_UP).multiply(TEN).setScale(2, RoundingMode.HALF_UP);
    }

    /** 结果分级（npss-rule §3.3）。 */
    public static ResultLevel level(BigDecimal weighted) {
        if (weighted.compareTo(NINETY) >= 0) {
            return ResultLevel.SUCCESS;
        }
        if (weighted.compareTo(SEVENTY) >= 0) {
            return ResultLevel.MIXED;
        }
        return ResultLevel.FAILURE;
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
