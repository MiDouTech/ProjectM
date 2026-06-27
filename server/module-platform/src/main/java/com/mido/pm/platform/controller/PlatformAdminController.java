package com.mido.pm.platform.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.platform.dto.AdminCreateDTO;
import com.mido.pm.platform.dto.AdminUpdateDTO;
import com.mido.pm.platform.dto.AdminVO;
import com.mido.pm.platform.dto.PlatformRoleVO;
import com.mido.pm.platform.dto.ResetPasswordDTO;
import com.mido.pm.platform.security.PlatformPerms;
import com.mido.pm.platform.service.PlatformAdminService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 平台账号与角色管理。 */
@Tag(name = "平台-账号与角色", description = "运营账号 CRUD、角色、重置密码")
@RestController
@RequestMapping("/api/v1/platform")
public class PlatformAdminController {

    private final PlatformAdminService adminService;

    public PlatformAdminController(PlatformAdminService adminService) {
        this.adminService = adminService;
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ADMIN_QUERY + "')")
    @Operation(summary = "角色列表")
    @GetMapping("/roles")
    public R<List<PlatformRoleVO>> roles() {
        return R.ok(adminService.listRoles());
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ADMIN_QUERY + "')")
    @Operation(summary = "运营账号列表")
    @GetMapping("/admins")
    public R<List<AdminVO>> list() {
        return R.ok(adminService.list());
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ADMIN_MANAGE + "')")
    @Operation(summary = "新建运营账号", description = "初始密码首登强制改密")
    @PostMapping("/admins")
    public R<Long> create(@Valid @RequestBody AdminCreateDTO dto) {
        return R.ok(adminService.create(dto));
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ADMIN_MANAGE + "')")
    @Operation(summary = "编辑运营账号", description = "改名/状态/角色，含提权护栏")
    @PutMapping("/admins/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AdminUpdateDTO dto) {
        adminService.update(id, dto);
        return R.ok();
    }

    @PreAuthorize("hasAuthority('" + PlatformPerms.ADMIN_MANAGE + "')")
    @Operation(summary = "重置账号密码", description = "重置后该账号下次登录强制改密")
    @PutMapping("/admins/{id}/password")
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        adminService.resetPassword(id, dto);
        return R.ok();
    }
}
