package com.mido.pm.briefing.controller;

import com.mido.pm.briefing.dto.BriefingTemplateVO;
import com.mido.pm.briefing.service.BriefingTemplateService;
import com.mido.pm.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{id}")
    public R<BriefingTemplateVO> get(@PathVariable Long id) {
        return R.ok(templateService.get(id));
    }
}
