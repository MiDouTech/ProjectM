package com.mido.pm.platform.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.platform.dto.PlanFeatureDTO;
import com.mido.pm.platform.dto.PlanFeatureVO;
import com.mido.pm.platform.security.PlatformPerms;
import com.mido.pm.platform.service.PlatformFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 套餐功能开关配置（运营侧）。 */
@Tag(name = "平台-套餐功能", description = "按套餐配置功能开关")
@RestController
@RequestMapping("/api/v1/platform/plans/{planId}/features")
public class PlatformPlanFeatureController {

    private final PlatformFeatureService featureService;

    public PlatformPlanFeatureController(PlatformFeatureService featureService) {
        this.featureService = featureService;
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.PLAN_QUERY + "')")
    @Operation(summary = "套餐功能列表", description = "返回该套餐各功能码启用态")
    @GetMapping
    public R<List<PlanFeatureVO>> list(@PathVariable Long planId) {
        return R.ok(featureService.featuresOfPlan(planId));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.FEATURE_MANAGE + "')")
    @Operation(summary = "保存套餐功能", description = "配置该套餐启用的功能码集合")
    @PutMapping
    public R<Void> save(@PathVariable Long planId, @Valid @RequestBody PlanFeatureDTO dto) {
        featureService.saveFeatures(planId, dto);
        return R.ok();
    }
}
