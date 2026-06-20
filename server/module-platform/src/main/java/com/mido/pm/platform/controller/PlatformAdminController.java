package com.mido.pm.platform.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.platform.dto.AdminCreateDTO;
import com.mido.pm.platform.dto.AdminUpdateDTO;
import com.mido.pm.platform.dto.AdminVO;
import com.mido.pm.platform.dto.PlatformRoleVO;
import com.mido.pm.platform.dto.ResetPasswordDTO;
import com.mido.pm.platform.security.PlatformPerms;
import com.mido.pm.platform.service.PlatformAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 平台账号与角色管理。 */
@RestController
@RequestMapping("/api/v1/platform")
public class PlatformAdminController {

    private final PlatformAdminService adminService;

    public PlatformAdminController(PlatformAdminService adminService) {
        this.adminService = adminService;
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ADMIN_QUERY + "')")
    @GetMapping("/roles")
    public R<List<PlatformRoleVO>> roles() {
        return R.ok(adminService.listRoles());
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ADMIN_QUERY + "')")
    @GetMapping("/admins")
    public R<List<AdminVO>> list() {
        return R.ok(adminService.list());
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ADMIN_MANAGE + "')")
    @PostMapping("/admins")
    public R<Long> create(@Valid @RequestBody AdminCreateDTO dto) {
        return R.ok(adminService.create(dto));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ADMIN_MANAGE + "')")
    @PutMapping("/admins/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AdminUpdateDTO dto) {
        adminService.update(id, dto);
        return R.ok();
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ADMIN_MANAGE + "')")
    @PutMapping("/admins/{id}/password")
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        adminService.resetPassword(id, dto);
        return R.ok();
    }
}
