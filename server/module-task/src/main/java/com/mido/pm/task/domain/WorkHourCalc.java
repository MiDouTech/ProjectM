package com.mido.pm.task.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 工时汇总口径（任务级/项目级/人员级统一）：
 * 进度 = 实际/预估*100（百分比，1 位小数，不封顶，超工可 &gt;100）；预估为 0 时进度按规则置 0，不报错。
 * 剩余 = 预估 − 实际（2 位小数，可为负，表示超工）。
 */
public final class WorkHourCalc {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private WorkHourCalc() {
    }

    /** 进度百分比；预估为 0 → 0（不抛除零异常）。 */
    public static BigDecimal progressPercent(BigDecimal est, BigDecimal actual) {
        BigDecimal e = nz(est);
        if (e.signum() == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return nz(actual).multiply(HUNDRED).divide(e, 1, RoundingMode.HALF_UP);
    }

    /** 剩余 = 预估 − 实际（可为负）。 */
    public static BigDecimal remaining(BigDecimal est, BigDecimal actual) {
        return nz(est).subtract(nz(actual)).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
