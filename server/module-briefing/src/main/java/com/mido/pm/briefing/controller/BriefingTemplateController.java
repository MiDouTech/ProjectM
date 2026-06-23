package com.mido.pm.briefing.controller;

import com.mido.pm.briefing.dto.AssignmentSaveDTO;
import com.mido.pm.briefing.dto.BriefingTemplateVO;
import com.mido.pm.briefing.dto.TemplateSaveDTO;
import com.mido.pm.briefing.service.BriefingTemplateService;
import com.mido.pm.common.api.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 简报模板：列表（惰性建内置日/周/月报）+ 详情。 */
@RestController
@RequestMapping("/api/v1/briefing-templates")
public class BriefingTemplateController {

    private final BriefingTemplateService templateService;

    public BriefingTemplateController(BriefingTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public R<List<BriefingTemplateVO>> list() {
        return R.ok(templateService.list());
    }

    /** 我应交的模板（指派到我本人或我部门）。 */
    @GetMapping("/assigned")
    public R<List<BriefingTemplateVO>> assigned() {
        return R.ok(templateService.assignedToMe());
    }

    @GetMapping("/{id}")
    public R<BriefingTemplateVO> get(@PathVariable Long id) {
        return R.ok(templateService.get(id));
    }

    /** 新建自定义模板。 */
    @PostMapping
    public R<Long> create(@Valid @RequestBody TemplateSaveDTO dto) {
        return R.ok(templateService.create(dto));
    }

    /** 更新自定义模板。 */
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TemplateSaveDTO dto) {
        templateService.update(id, dto);
        return R.ok();
    }

    /** 停用自定义模板。 */
    @DeleteMapping("/{id}")
    public R<Void> disable(@PathVariable Long id) {
        templateService.disable(id);
        return R.ok();
    }

    /** 设置模板指派（覆盖用户/部门）。 */
    @PutMapping("/{id}/assignments")
    public R<Void> assign(@PathVariable Long id, @RequestBody AssignmentSaveDTO dto) {
        templateService.setAssignments(id, dto);
        return R.ok();
    }
}
