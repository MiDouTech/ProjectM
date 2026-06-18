package com.mido.pm.report.dto;

import java.math.BigDecimal;
import java.util.List;

/** 度量总览（范围内）：任务完成率/逾期率 + 项目按 S/I/O 分布。 */
public record MetricsOverviewVO(
        long taskTotal,
        long completed,
        long overdue,
        BigDecimal completionRate,
        BigDecimal overdueRate,
        List<CategoryCount> categoryDistribution) {

    /** 项目类型分布项（S/I/O）。 */
    public record CategoryCount(String category, long count) {
    }
}
