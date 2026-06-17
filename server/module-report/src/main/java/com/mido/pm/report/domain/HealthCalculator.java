package com.mido.pm.report.domain;

import java.math.BigDecimal;

/**
 * 项目健康度（进度/预算/逾期综合，纯函数）。入参均为百分比（0-100，budgetUsage 可 &gt;100 表示超预算）。
 * 规则（阈值集中登记）：
 * - 风险(red)：逾期率 &gt; 30% 或 预算使用 &gt; 100%（超预算）。
 * - 关注(yellow)：逾期率 &gt; 10% 或 预算使用 &gt; 90% 或 完成率 &lt; 50%。
 * - 否则 健康(green)。
 */
public final class HealthCalculator {

    private static final BigDecimal OVERDUE_RED = BigDecimal.valueOf(30);
    private static final BigDecimal OVERDUE_YELLOW = BigDecimal.valueOf(10);
    private static final BigDecimal BUDGET_RED = BigDecimal.valueOf(100);
    private static final BigDecimal BUDGET_YELLOW = BigDecimal.valueOf(90);
    private static final BigDecimal COMPLETION_YELLOW = BigDecimal.valueOf(50);

    private HealthCalculator() {
    }

    public static HealthLevel evaluate(BigDecimal completionRate, BigDecimal overdueRate, BigDecimal budgetUsage) {
        BigDecimal cr = nz(completionRate);
        BigDecimal od = nz(overdueRate);
        BigDecimal bu = nz(budgetUsage);
        if (od.compareTo(OVERDUE_RED) > 0 || bu.compareTo(BUDGET_RED) > 0) {
            return HealthLevel.RED;
        }
        if (od.compareTo(OVERDUE_YELLOW) > 0 || bu.compareTo(BUDGET_YELLOW) > 0
                || cr.compareTo(COMPLETION_YELLOW) < 0) {
            return HealthLevel.YELLOW;
        }
        return HealthLevel.GREEN;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
