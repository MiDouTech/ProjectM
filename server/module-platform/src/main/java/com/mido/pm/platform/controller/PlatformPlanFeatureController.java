package com.mido.pm.platform.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.platform.dto.PlanFeatureDTO;
import com.mido.pm.platform.dto.PlanFeatureVO;
import com.mido.pm.platform.security.PlatformPerms;
import com.mido.pm.platform.service.PlatformFeatureService;
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
@RestController
@RequestMapping("/api/v1/platform/plans/{planId}/features")
public class PlatformPlanFeatureController {

    private final PlatformFeatureService featureService;

    public PlatformPlanFeatureController(PlatformFeatureService featureService) {
        this.featureService = featureService;
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.PLAN_QUERY + "')")
    @GetMapping
    public R<List<PlanFeatureVO>> list(@PathVariable Long planId) {
        return R.ok(featureService.featuresOfPlan(planId));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.FEATURE_MANAGE + "')")
    @PutMapping
    public R<Void> save(@PathVariable Long planId, @Valid @RequestBody PlanFeatureDTO dto) {
        featureService.saveFeatures(planId, dto);
        return R.ok();
    }
}
