package com.mido.pm.report.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.report.dto.BurndownVO;
import com.mido.pm.report.dto.MetricsOverviewVO;
import com.mido.pm.report.dto.ProjectHealthVO;
import com.mido.pm.report.service.ReportMetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 项目/任务度量（只读，数据范围由全局拦截器约束）。 */
@RestController
@RequestMapping("/api/v1/reports/metrics")
public class ReportMetricsController {

    private final ReportMetricsService metricsService;

    public ReportMetricsController(ReportMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    /** 总览：任务完成率/逾期率 + 项目 S/I/O 分布。 */
    @GetMapping("/overview")
    public R<MetricsOverviewVO> overview() {
        return R.ok(metricsService.overview());
    }

    /** 燃尽图数据（按项目）。 */
    @GetMapping("/burndown")
    public R<BurndownVO> burndown(@RequestParam Long projectId) {
        return R.ok(metricsService.burndown(projectId));
    }

    /** 项目健康度（进度/预算/逾期综合）。 */
    @GetMapping("/project-health")
    public R<ProjectHealthVO> projectHealth(@RequestParam Long projectId) {
        return R.ok(metricsService.projectHealth(projectId));
    }
}
