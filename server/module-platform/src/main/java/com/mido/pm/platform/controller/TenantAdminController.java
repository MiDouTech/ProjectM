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
    @PostMapping("/query")
    public R<PageResult<TenantVO>> query(@RequestBody TenantQueryDTO query) {
        return R.ok(tenantService.page(query));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_QUERY + "')")
    @GetMapping("/{id}")
    public R<TenantDetailVO> detail(@PathVariable Long id) {
        return R.ok(tenantService.detail(id));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_MANAGE + "')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody TenantCreateDTO dto) {
        return R.ok(tenantService.create(dto));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_MANAGE + "')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TenantUpdateDTO dto) {
        tenantService.update(id, dto);
        return R.ok();
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_MANAGE + "')")
    @PutMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody TenantStatusDTO dto) {
        tenantService.changeStatus(id, dto);
        return R.ok();
    }

    /** 批量状态流转（批量启用/停用）。 */
    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_MANAGE + "')")
    @PostMapping("/batch-status")
    public R<Integer> batchChangeStatus(@Valid @RequestBody TenantBatchStatusDTO dto) {
        return R.ok(tenantService.batchChangeStatus(dto));
    }

    /** 租户用量（用量 vs 套餐配额，含是否超限）。 */
    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_QUERY + "')")
    @GetMapping("/{id}/usage")
    public R<List<TenantUsageVO>> usage(@PathVariable Long id) {
        return R.ok(usageService.usageOf(id));
    }

    /** 模拟登录进租户排障：返回短时租户令牌（高敏感，全程审计）。 */
    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_IMPERSONATE + "')")
    @PostMapping("/{id}/impersonate")
    public R<ImpersonateVO> impersonate(@PathVariable Long id) {
        return R.ok(impersonationService.impersonate(id));
    }

    /** 发起数据导出（异步，核心域 JSON）。 */
    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_MANAGE + "')")
    @PostMapping("/{id}/export")
    public R<Long> requestExport(@PathVariable Long id) {
        return R.ok(exportService.requestExport(id));
    }

    /** 导出任务列表。 */
    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_QUERY + "')")
    @GetMapping("/{id}/exports")
    public R<List<ExportTaskVO>> exports(@PathVariable Long id) {
        List<ExportTaskVO> list = exportService.list(id).stream().map(this::toExportVO).toList();
        return R.ok(list);
    }

    /** 取导出文件下载 URL（限时预签名）。 */
    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_QUERY + "')")
    @GetMapping("/{id}/exports/{exportId}/download")
    public R<Map<String, String>> download(@PathVariable Long id, @PathVariable Long exportId) {
        return R.ok(Map.of("url", exportService.downloadUrl(exportId)));
    }

    /** 发起注销（标记 closed + 计划清除，graceDays 缺省取配置默认）。 */
    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_MANAGE + "')")
    @PostMapping("/{id}/deletion")
    public R<Void> requestDeletion(@PathVariable Long id, @RequestParam(required = false) Integer graceDays) {
        deletionService.requestDeletion(id, graceDays);
        return R.ok();
    }

    /** 宽限期内取消注销。 */
    @PreAuthorize("hasAuthority('" + PlatformPerms.TENANT_MANAGE + "')")
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
