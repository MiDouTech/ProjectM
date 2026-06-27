package com.mido.pm.platform.controller;

import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.api.R;
import com.mido.pm.platform.dto.RevenueQueryDTO;
import com.mido.pm.platform.dto.RevenueRecordDTO;
import com.mido.pm.platform.dto.RevenueSummaryVO;
import com.mido.pm.platform.dto.RevenueVO;
import com.mido.pm.platform.security.PlatformPerms;
import com.mido.pm.platform.service.PlatformRevenueService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 线下收入台账。 */
@Tag(name = "平台-收入台账", description = "线下收款/退款流水与汇总")
@RestController
@RequestMapping("/api/v1/platform/revenue")
public class PlatformRevenueController {

    private final PlatformRevenueService revenueService;

    public PlatformRevenueController(PlatformRevenueService revenueService) {
        this.revenueService = revenueService;
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.REVENUE_QUERY + "')")
    @Operation(summary = "收入流水分页查询")
    @PostMapping("/query")
    public R<PageResult<RevenueVO>> query(@RequestBody RevenueQueryDTO query) {
        return R.ok(revenueService.page(query));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.REVENUE_QUERY + "')")
    @Operation(summary = "收入汇总", description = "收款/退款/净额")
    @GetMapping("/summary")
    public R<RevenueSummaryVO> summary(@RequestParam(required = false) Long tenantId) {
        return R.ok(revenueService.summary(tenantId));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.REVENUE_MANAGE + "')")
    @Operation(summary = "新增收入记录", description = "退款不超已收净额")
    @PostMapping
    public R<Long> create(@Valid @RequestBody RevenueRecordDTO dto) {
        return R.ok(revenueService.create(dto));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.REVENUE_MANAGE + "')")
    @Operation(summary = "编辑收入记录")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RevenueRecordDTO dto) {
        revenueService.update(id, dto);
        return R.ok();
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.REVENUE_MANAGE + "')")
    @Operation(summary = "删除收入记录")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        revenueService.delete(id);
        return R.ok();
    }
}
