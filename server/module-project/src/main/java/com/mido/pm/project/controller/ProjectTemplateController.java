package com.mido.pm.project.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.project.dto.TemplateDetailVO;
import com.mido.pm.project.dto.TemplateSaveDTO;
import com.mido.pm.project.dto.TemplateVO;
import com.mido.pm.project.service.ProjectTemplateService;
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

/** 项目模板（内置 5 套 + 自定义）：查询 + 自定义模板增删改。内置模板禁改禁删。 */
@RestController
@RequestMapping("/api/v1/project-templates")
public class ProjectTemplateController {

    private final ProjectTemplateService templateService;

    public ProjectTemplateController(ProjectTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public R<List<TemplateVO>> list(@RequestParam(required = false) String category) {
        return R.ok(templateService.list(category));
    }

    @GetMapping("/{id}")
    public R<TemplateVO> get(@PathVariable Long id) {
        return R.ok(templateService.get(id));
    }

    /** 模板详情（含 config，编辑回显）。 */
    @GetMapping("/{id}/detail")
    public R<TemplateDetailVO> detail(@PathVariable Long id) {
        return R.ok(templateService.detail(id));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody TemplateSaveDTO dto) {
        return R.ok(templateService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TemplateSaveDTO dto) {
        templateService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return R.ok();
    }
}
