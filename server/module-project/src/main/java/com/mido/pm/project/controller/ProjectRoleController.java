package com.mido.pm.project.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.project.dto.ProjectRoleSaveDTO;
import com.mido.pm.project.dto.ProjectRoleVO;
import com.mido.pm.project.service.ProjectRoleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 项目角色 CRUD（租户自配）。 */
@RestController
@RequestMapping("/api/v1/project-roles")
public class ProjectRoleController {

    private final ProjectRoleService roleService;

    public ProjectRoleController(ProjectRoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public R<List<ProjectRoleVO>> list(@RequestParam(defaultValue = "false") boolean onlyActive) {
        return R.ok(roleService.list(onlyActive));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody ProjectRoleSaveDTO dto) {
        return R.ok(roleService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ProjectRoleSaveDTO dto) {
        roleService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }
}
