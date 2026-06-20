package com.mido.pm.platform.controller;

import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.api.R;
import com.mido.pm.platform.dto.TenantCreateDTO;
import com.mido.pm.platform.dto.TenantDetailVO;
import com.mido.pm.platform.dto.TenantQueryDTO;
import com.mido.pm.platform.dto.TenantStatusDTO;
import com.mido.pm.platform.dto.TenantUpdateDTO;
import com.mido.pm.platform.dto.TenantVO;
import com.mido.pm.platform.security.PlatformPerms;
import com.mido.pm.platform.service.TenantAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 租户管理：开通/编辑/状态流转/详情。 */
@RestController
@RequestMapping("/api/v1/platform/tenants")
public class TenantAdminController {

    private final TenantAdminService tenantService;

    public TenantAdminController(TenantAdminService tenantService) {
        this.tenantService = tenantService;
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
}
