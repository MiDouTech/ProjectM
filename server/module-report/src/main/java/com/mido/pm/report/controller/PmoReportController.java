package com.mido.pm.report.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.report.dto.PmoNpssVO;
import com.mido.pm.report.service.PmoReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** PMO 报表：总体评价（按财年 成功%−失败%，对比基线 36）。 */
@RestController
@RequestMapping("/api/v1/reports")
public class PmoReportController {

    private final PmoReportService pmoReportService;

    public PmoReportController(PmoReportService pmoReportService) {
        this.pmoReportService = pmoReportService;
    }

    /** PMO 总体评价；year 不传默认当前自然年。 */
    @GetMapping("/pmo-npss")
    public R<PmoNpssVO> pmoNpss(@RequestParam(required = false) Integer year) {
        return R.ok(pmoReportService.pmoNpss(year == null ? LocalDate.now().getYear() : year));
    }
}
