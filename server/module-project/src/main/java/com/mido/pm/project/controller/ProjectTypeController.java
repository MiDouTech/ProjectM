package com.mido.pm.project.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.project.dto.ProjectTypeSaveDTO;
import com.mido.pm.project.dto.ProjectTypeVO;
import com.mido.pm.project.service.ProjectTypeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 项目类型管理（SaaS 租户自配，取代硬编码枚举 S/I/O）。 */
@RestController
@RequestMapping("/api/v1/project-types")
public class ProjectTypeController {

    private final ProjectTypeService typeService;

    public ProjectTypeController(ProjectTypeService typeService) {
        this.typeService = typeService;
    }

    /** 类型列表；onlyActive=true 仅启用态（默认 false 返回全部，供管理端）。 */
    @GetMapping
    public R<List<ProjectTypeVO>> list(@RequestParam(required = false, defaultValue = "false") boolean onlyActive) {
        return R.ok(typeService.list(onlyActive));
    }

    @GetMapping("/{id}")
    public R<ProjectTypeVO> get(@PathVariable Long id) {
        return R.ok(typeService.get(id));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody ProjectTypeSaveDTO dto) {
        return R.ok(typeService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ProjectTypeSaveDTO dto) {
        typeService.update(id, dto);
        return R.ok();
    }

    /** 启用/停用：active=false 停用。 */
    @PutMapping("/{id}/status")
    public R<Void> setStatus(@PathVariable Long id, @RequestParam boolean active) {
        typeService.setStatus(id, active);
        return R.ok();
    }
}
