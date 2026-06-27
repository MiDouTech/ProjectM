package com.mido.pm.platform.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.common.tenant.TenantContext;
import com.mido.pm.platform.service.PlatformFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 租户侧功能开关查询（走租户安全链）。返回当前租户据其生效套餐启用的功能码，供前端门控。
 */
@Tag(name = "租户-功能开关", description = "按生效套餐返回启用功能码")
@RestController
@RequestMapping("/api/v1/features")
public class TenantFeatureController {

    private final PlatformFeatureService featureService;

    public TenantFeatureController(PlatformFeatureService featureService) {
        this.featureService = featureService;
    }

    @Operation(summary = "启用功能码", description = "供前端按套餐门控功能")
    @GetMapping
    public R<List<String>> enabled() {
        return R.ok(featureService.enabledFeaturesForTenant(TenantContext.get()));
    }
}
