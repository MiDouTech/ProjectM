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

    /** 一个评价主体：权重 + 组内成员各自打分（用于"组内平均→主体加权"，npss-rule §3 评价主体口径）。 */
    public record SubjectScores(BigDecimal weight, List<Integer> memberScores) {
    }

    private NpssCalculator() {
    }

    /**
     * 评价主体口径加权满意度（0-100，2 位）：组内先取平均，再按主体权重加权求和，权重归一化后 ×10。
     * 同一主体多人 → 取分数平均再加权（需求与 npss-rule §3）。无成员评分的主体跳过，其权重不参与归一化。
     * 无任何有效主体或权重和为 0 → 0。
     */
    public static BigDecimal weightedSatisfactionBySubject(List<SubjectScores> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            return zero();
        }
        BigDecimal sumW = BigDecimal.ZERO;
        BigDecimal sumSW = BigDecimal.ZERO;
        for (SubjectScores s : subjects) {
            if (s.memberScores() == null || s.memberScores().isEmpty()) {
                continue; // 空主体跳过（权重不计入归一化）
            }
            BigDecimal w = s.weight() == null ? BigDecimal.ZERO : s.weight();
            BigDecimal avg = averageScore(s.memberScores()); // 组内平均
            sumW = sumW.add(w);
            sumSW = sumSW.add(w.multiply(avg));
        }
        if (sumW.signum() == 0) {
            return zero();
        }
        // (Σ(w_j × avg_j) / Σw_j) × 10
        return sumSW.divide(sumW, 6, RoundingMode.HALF_UP).multiply(TEN).setScale(2, RoundingMode.HALF_UP);
    }

    /** 组内平均分（0-10，6 位中间精度）。 */
    private static BigDecimal averageScore(List<Integer> scores) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Integer sc : scores) {
            sum = sum.add(BigDecimal.valueOf(sc == null ? 0 : sc));
        }
        return sum.divide(BigDecimal.valueOf(scores.size()), 6, RoundingMode.HALF_UP);
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
