package com.mido.pm.report.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.report.dto.ReportSettingUpdateDTO;
import com.mido.pm.report.dto.ReportSettingVO;
import com.mido.pm.report.service.ReportSettingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 报表设置（租户级）：财年起始月。供 PMO 财年口径使用。 */
@RestController
@RequestMapping("/api/v1/reports/settings")
public class ReportSettingController {

    private final ReportSettingService reportSettingService;

    public ReportSettingController(ReportSettingService reportSettingService) {
        this.reportSettingService = reportSettingService;
    }

    @GetMapping
    public R<ReportSettingVO> get() {
        return R.ok(reportSettingService.get());
    }

    @PutMapping
    public R<Void> save(@Valid @RequestBody ReportSettingUpdateDTO dto) {
        reportSettingService.setFiscalYearStartMonth(dto.fiscalYearStartMonth());
        return R.ok();
    }
}
