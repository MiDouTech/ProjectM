package com.mido.pm.platform.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.platform.dto.PlanSaveDTO;
import com.mido.pm.platform.dto.PlanVO;
import com.mido.pm.platform.security.PlatformPerms;
import com.mido.pm.platform.service.PlatformPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "平台-套餐与配额", description = "套餐 CRUD 与配额项")
@RestController
@RequestMapping("/api/v1/platform/plans")
public class PlatformPlanController {

    private final PlatformPlanService planService;

    public PlatformPlanController(PlatformPlanService planService) {
        this.planService = planService;
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.PLAN_QUERY + "')")
    @Operation(summary = "套餐列表")
    @GetMapping
    public R<List<PlanVO>> list() {
        return R.ok(planService.list());
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.PLAN_QUERY + "')")
    @Operation(summary = "套餐详情")
    @GetMapping("/{id}")
    public R<PlanVO> get(@PathVariable Long id) {
        return R.ok(planService.get(id));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.PLAN_MANAGE + "')")
    @Operation(summary = "新建套餐")
    @PostMapping
    public R<Long> create(@Valid @RequestBody PlanSaveDTO dto) {
        return R.ok(planService.create(dto));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.PLAN_MANAGE + "')")
    @Operation(summary = "编辑套餐")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PlanSaveDTO dto) {
        planService.update(id, dto);
        return R.ok();
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.PLAN_MANAGE + "')")
    @Operation(summary = "删除套餐", description = "被订阅的套餐不可删")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return R.ok();
    }
}
