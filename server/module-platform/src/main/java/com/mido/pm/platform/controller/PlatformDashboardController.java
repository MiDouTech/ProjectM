package com.mido.pm.platform.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.platform.dto.DashboardVO;
import com.mido.pm.platform.security.PlatformPerms;
import com.mido.pm.platform.service.PlatformDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 运营概览。 */
@Tag(name = "平台-运营概览", description = "租户总览指标与到期预警")
@RestController
@RequestMapping("/api/v1/platform/dashboard")
public class PlatformDashboardController {

    private final PlatformDashboardService dashboardService;

    public PlatformDashboardController(PlatformDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.DASHBOARD_VIEW + "')")
    @Operation(summary = "运营概览", description = "租户总数/状态分布/近30天到期")
    @GetMapping("/overview")
    public R<DashboardVO> overview() {
        return R.ok(dashboardService.overview());
    }
}
