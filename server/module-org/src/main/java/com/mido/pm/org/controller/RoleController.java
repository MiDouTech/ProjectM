package com.mido.pm.org.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.org.dto.DataScopeSettingDTO;
import com.mido.pm.org.dto.FieldPermSettingDTO;
import com.mido.pm.org.dto.RoleCreateDTO;
import com.mido.pm.org.dto.RoleUpdateDTO;
import com.mido.pm.org.dto.RoleVO;
import com.mido.pm.org.service.SysRoleService;
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

/** 角色 CRUD + 配权限码 + 配数据范围。 */
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final SysRoleService roleService;

    public RoleController(SysRoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public R<List<RoleVO>> list() {
        return R.ok(roleService.list());
    }

    @GetMapping("/{id}")
    public R<RoleVO> get(@PathVariable Long id) {
        return R.ok(roleService.get(id));
    }

    @PreAuthorize("hasAuthority('org:role:create')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody RoleCreateDTO dto) {
        return R.ok(roleService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        roleService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}/perms")
    public R<List<String>> getPerms(@PathVariable Long id) {
        return R.ok(roleService.getPerms(id));
    }

    @PutMapping("/{id}/perms")
    public R<Void> savePerms(@PathVariable Long id, @RequestBody List<String> permCodes) {
        roleService.savePerms(id, permCodes);
        return R.ok();
    }

    @GetMapping("/{id}/data-scopes")
    public R<List<DataScopeSettingDTO>> getDataScopes(@PathVariable Long id) {
        return R.ok(roleService.getDataScopes(id));
    }

    @PutMapping("/{id}/data-scopes")
    public R<Void> saveDataScopes(@PathVariable Long id, @RequestBody List<DataScopeSettingDTO> settings) {
        roleService.saveDataScopes(id, settings);
        return R.ok();
    }

    @GetMapping("/{id}/custom-depts")
    public R<List<Long>> getCustomDepts(@PathVariable Long id) {
        return R.ok(roleService.getCustomDepts(id));
    }

    @PreAuthorize("hasAuthority('org:role:create')")
    @PutMapping("/{id}/custom-depts")
    public R<Void> saveCustomDepts(@PathVariable Long id, @RequestBody List<Long> deptIds) {
        roleService.saveCustomDepts(id, deptIds);
        return R.ok();
    }

    @GetMapping("/{id}/field-perms")
    public R<List<FieldPermSettingDTO>> getFieldPerms(@PathVariable Long id) {
        return R.ok(roleService.getFieldPerms(id));
    }

    @PreAuthorize("hasAuthority('org:role:create')")
    @PutMapping("/{id}/field-perms")
    public R<Void> saveFieldPerms(@PathVariable Long id, @RequestBody List<FieldPermSettingDTO> settings) {
        roleService.saveFieldPerms(id, settings);
        return R.ok();
    }
}
