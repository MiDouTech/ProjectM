package com.mido.pm.platform.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.platform.security.PlatformPerms;
import com.mido.pm.platform.service.PlatformUsageService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 用量快照手动触发（日常由定时任务自动执行）。 */
@RestController
@RequestMapping("/api/v1/platform/usage")
public class PlatformUsageController {

    private final PlatformUsageService usageService;

    public PlatformUsageController(PlatformUsageService usageService) {
        this.usageService = usageService;
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_MANAGE + "')")
    @PostMapping("/snapshot")
    public R<Integer> snapshot() {
        return R.ok(usageService.snapshotAll());
    }
}
