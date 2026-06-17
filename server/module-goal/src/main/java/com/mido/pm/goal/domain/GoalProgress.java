package com.mido.pm.goal.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * KR 量化进度计算（纯函数）：progress = (current-start)/(target-start)，结果钳制到 [0,100]（百分比，2 位）。
 * 边界保护：start/target/current 任一为空、或 target==start（无可测区间）→ 0，不抛除零。
 * 同时天然支持递减型指标（如 start=100→target=10）：分子分母同号，比值仍正确。
 */
public final class GoalProgress {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private GoalProgress() {
    }

    public static BigDecimal compute(BigDecimal start, BigDecimal target, BigDecimal current) {
        if (start == null || target == null || current == null || target.compareTo(start) == 0) {
            return ZERO;
        }
        BigDecimal ratio = current.subtract(start)
                .divide(target.subtract(start), 6, RoundingMode.HALF_UP);
        if (ratio.signum() < 0) {
            ratio = BigDecimal.ZERO;
        } else if (ratio.compareTo(BigDecimal.ONE) > 0) {
            ratio = BigDecimal.ONE;
        }
        return ratio.multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP);
    }
}
