package com.mido.pm.platform.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.platform.dto.PlanSaveDTO;
import com.mido.pm.platform.dto.PlanVO;
import com.mido.pm.platform.security.PlatformPerms;
import com.mido.pm.platform.service.PlatformPlanService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 套餐与配额管理。 */
@RestController
@RequestMapping("/api/v1/platform/plans")
public class PlatformPlanController {

    private final PlatformPlanService planService;

    public PlatformPlanController(PlatformPlanService planService) {
        this.planService = planService;
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.PLAN_QUERY + "')")
    @GetMapping
    public R<List<PlanVO>> list() {
        return R.ok(planService.list());
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.PLAN_QUERY + "')")
    @GetMapping("/{id}")
    public R<PlanVO> get(@PathVariable Long id) {
        return R.ok(planService.get(id));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.PLAN_MANAGE + "')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody PlanSaveDTO dto) {
        return R.ok(planService.create(dto));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.PLAN_MANAGE + "')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PlanSaveDTO dto) {
        planService.update(id, dto);
        return R.ok();
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.PLAN_MANAGE + "')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return R.ok();
    }
}
