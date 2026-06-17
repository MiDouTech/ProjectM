package com.mido.pm.report.service;

import com.mido.pm.report.domain.HealthCalculator;
import com.mido.pm.report.domain.HealthLevel;
import com.mido.pm.report.domain.MetricsCalculator;
import com.mido.pm.report.dto.BurndownVO;
import com.mido.pm.report.dto.BurndownVO.BurndownPoint;
import com.mido.pm.report.dto.MetricsOverviewVO;
import com.mido.pm.report.dto.MetricsOverviewVO.CategoryCount;
import com.mido.pm.report.dto.ProjectHealthVO;
import com.mido.pm.report.mapper.ReportMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 项目/任务度量（只读聚合）。任务完成率/逾期率、燃尽图、项目健康度、S/I/O 分布。
 * 数据范围与租户由全局拦截器在 SQL 注入；阶段一 MySQL 聚合，后续可切 ES（{@link ReportMapper} 即替换点）。
 */
@Service
public class ReportMetricsService {

    private final ReportMapper reportMapper;

    public ReportMetricsService(ReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    public MetricsOverviewVO overview() {
        Map<String, Object> t = reportMapper.taskCounts();
        long total = lng(t, "total");
        long completed = lng(t, "completed");
        long overdue = lng(t, "overdue");
        List<CategoryCount> dist = reportMapper.categoryDistribution().stream()
                .map(m -> new CategoryCount(str(m, "category"), lng(m, "cnt")))
                .toList();
        return new MetricsOverviewVO(total, completed, overdue,
                MetricsCalculator.rate(completed, total), MetricsCalculator.rate(overdue, total), dist);
    }

    public BurndownVO burndown(Long projectId) {
        List<Map<String, Object>> buckets = reportMapper.dueBuckets(projectId);
        long total = buckets.stream().mapToLong(m -> lng(m, "cnt")).sum();
        List<BurndownPoint> points = new ArrayList<>();
        long cumulative = 0;
        for (Map<String, Object> b : buckets) {
            cumulative += lng(b, "cnt");
            points.add(new BurndownPoint(str(b, "dueDate"), total - cumulative));
        }
        return new BurndownVO(total, points);
    }

    public ProjectHealthVO projectHealth(Long projectId) {
        Map<String, Object> t = reportMapper.taskCountsByProject(projectId);
        long total = lng(t, "total");
        long completed = lng(t, "completed");
        long overdue = lng(t, "overdue");
        BigDecimal completionRate = MetricsCalculator.rate(completed, total);
        BigDecimal overdueRate = MetricsCalculator.rate(overdue, total);

        Map<String, Object> b = reportMapper.projectBudget(projectId);
        BigDecimal budget = big(b, "budget");
        BigDecimal actual = big(b, "actualCost");
        BigDecimal budgetUsage = (budget == null || budget.signum() == 0) ? null
                : actual.multiply(BigDecimal.valueOf(100)).divide(budget, 2, RoundingMode.HALF_UP);

        HealthLevel level = HealthCalculator.evaluate(completionRate, overdueRate, budgetUsage);
        return new ProjectHealthVO(projectId, total, completionRate, overdueRate, budgetUsage,
                level.name().toLowerCase(), level.getLabel());
    }

    // ===== Map 取值（COUNT→Long，SUM→BigDecimal，统一按 Number 处理） =====

    private static long lng(Map<String, Object> m, String key) {
        Object v = m == null ? null : m.get(key);
        return v instanceof Number n ? n.longValue() : 0L;
    }

    private static BigDecimal big(Map<String, Object> m, String key) {
        Object v = m == null ? null : m.get(key);
        if (v == null) {
            return null;
        }
        return v instanceof BigDecimal bd ? bd : new BigDecimal(v.toString());
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m == null ? null : m.get(key);
        return v == null ? null : v.toString();
    }
}
