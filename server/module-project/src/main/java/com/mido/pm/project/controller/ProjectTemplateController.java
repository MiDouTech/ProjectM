package com.mido.pm.project.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.project.dto.TemplateVO;
import com.mido.pm.project.service.ProjectTemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 项目模板查询（内置 5 套 + 自定义）。 */
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
}
