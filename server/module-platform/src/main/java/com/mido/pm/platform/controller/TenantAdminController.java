package com.mido.pm.platform.controller;

import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.api.R;
import com.mido.pm.platform.dto.ExportTaskVO;
import com.mido.pm.platform.dto.ImpersonateVO;
import com.mido.pm.platform.dto.TenantCreateDTO;
import com.mido.pm.platform.dto.TenantDetailVO;
import com.mido.pm.platform.dto.TenantQueryDTO;
import com.mido.pm.platform.dto.TenantBatchStatusDTO;
import com.mido.pm.platform.dto.TenantStatusDTO;
import com.mido.pm.platform.dto.TenantUpdateDTO;
import com.mido.pm.platform.dto.TenantUsageVO;
import com.mido.pm.platform.dto.TenantVO;
import com.mido.pm.platform.entity.SysTenantExport;
import com.mido.pm.platform.security.PlatformPerms;
import com.mido.pm.platform.service.PlatformDeletionService;
import com.mido.pm.platform.service.PlatformExportService;
import com.mido.pm.platform.service.PlatformImpersonationService;
import com.mido.pm.platform.service.PlatformUsageService;
import com.mido.pm.platform.service.TenantAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 租户管理：开通/编辑/状态流转/详情/用量/模拟登录/导出/注销。 */
@Tag(name = "平台-租户管理", description = "开通/编辑/状态/用量/模拟/导出/注销")
@RestController
@RequestMapping("/api/v1/platform/tenants")
public class TenantAdminController {

    private final TenantAdminService tenantService;
    private final PlatformUsageService usageService;
    private final PlatformImpersonationService impersonationService;
    private final PlatformExportService exportService;
    private final PlatformDeletionService deletionService;

    public TenantAdminController(TenantAdminService tenantService, PlatformUsageService usageService,
                                 PlatformImpersonationService impersonationService,
                                 PlatformExportService exportService,
                                 PlatformDeletionService deletionService) {
        this.tenantService = tenantService;
        this.usageService = usageService;
        this.impersonationService = impersonationService;
        this.exportService = exportService;
        this.deletionService = deletionService;
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_QUERY + "')")
    @Operation(summary = "租户分页查询")
    @PostMapping("/query")
    public R<PageResult<TenantVO>> query(@RequestBody TenantQueryDTO query) {
        return R.ok(tenantService.page(query));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_QUERY + "')")
    @Operation(summary = "租户详情", description = "含订阅与配额")
    @GetMapping("/{id}")
    public R<TenantDetailVO> detail(@PathVariable Long id) {
        return R.ok(tenantService.detail(id));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_MANAGE + "')")
    @Operation(summary = "开通租户", description = "初始为 trial，播种默认数据")
    @PostMapping
    public R<Long> create(@Valid @RequestBody TenantCreateDTO dto) {
        return R.ok(tenantService.create(dto));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_MANAGE + "')")
    @Operation(summary = "编辑租户")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TenantUpdateDTO dto) {
        tenantService.update(id, dto);
        return R.ok();
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_MANAGE + "')")
    @Operation(summary = "租户状态流转", description = "启用/停用/注销")
    @PutMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody TenantStatusDTO dto) {
        tenantService.changeStatus(id, dto);
        return R.ok();
    }

    /** 批量状态流转（批量启用/停用）。 */
    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_MANAGE + "')")
    @Operation(summary = "批量状态流转", description = "批量启用/停用")
    @PostMapping("/batch-status")
    public R<Integer> batchChangeStatus(@Valid @RequestBody TenantBatchStatusDTO dto) {
        return R.ok(tenantService.batchChangeStatus(dto));
    }

    /** 租户用量（用量 vs 套餐配额，含是否超限）。 */
    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_QUERY + "')")
    @Operation(summary = "租户用量", description = "用量 vs 配额含超限")
    @GetMapping("/{id}/usage")
    public R<List<TenantUsageVO>> usage(@PathVariable Long id) {
        return R.ok(usageService.usageOf(id));
    }

    /** 模拟登录进租户排障：返回短时租户令牌（高敏感，全程审计）。 */
    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_IMPERSONATE + "')")
    @Operation(summary = "模拟登录", description = "签发短时只读租户令牌，全程审计")
    @PostMapping("/{id}/impersonate")
    public R<ImpersonateVO> impersonate(@PathVariable Long id) {
        return R.ok(impersonationService.impersonate(id));
    }

    /** 发起数据导出（异步，核心域 JSON）。 */
    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_MANAGE + "')")
    @Operation(summary = "发起数据导出", description = "异步任务，完成后限时下载")
    @PostMapping("/{id}/export")
    public R<Long> requestExport(@PathVariable Long id) {
        return R.ok(exportService.requestExport(id));
    }

    /** 导出任务列表。 */
    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_QUERY + "')")
    @Operation(summary = "导出任务列表")
    @GetMapping("/{id}/exports")
    public R<List<ExportTaskVO>> exports(@PathVariable Long id) {
        List<ExportTaskVO> list = exportService.list(id).stream().map(this::toExportVO).toList();
        return R.ok(list);
    }

    /** 取导出文件下载 URL（限时预签名）。 */
    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_QUERY + "')")
    @Operation(summary = "导出下载地址", description = "返回限时预签名 URL")
    @GetMapping("/{id}/exports/{exportId}/download")
    public R<Map<String, String>> download(@PathVariable Long id, @PathVariable Long exportId) {
        return R.ok(Map.of("url", exportService.downloadUrl(exportId)));
    }

    /** 发起注销（标记 closed + 计划清除，graceDays 缺省取配置默认）。 */
    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_MANAGE + "')")
    @Operation(summary = "发起注销", description = "进入清除宽限期并自动导出备份")
    @PostMapping("/{id}/deletion")
    public R<Void> requestDeletion(@PathVariable Long id, @RequestParam(required = false) Integer graceDays) {
        deletionService.requestDeletion(id, graceDays);
        return R.ok();
    }

    /** 宽限期内取消注销。 */
    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_MANAGE + "')")
    @Operation(summary = "取消注销", description = "宽限期内撤销，状态置 suspended")
    @PostMapping("/{id}/deletion/cancel")
    public R<Void> cancelDeletion(@PathVariable Long id) {
        deletionService.cancelDeletion(id);
        return R.ok();
    }

    private ExportTaskVO toExportVO(SysTenantExport t) {
        boolean ready = "done".equals(t.getStatus()) && t.getFileKey() != null;
        return new ExportTaskVO(t.getId(), t.getTenantId(), t.getStatus(), ready,
                t.getError(), t.getCreateTime(), t.getUpdateTime());
    }
}
