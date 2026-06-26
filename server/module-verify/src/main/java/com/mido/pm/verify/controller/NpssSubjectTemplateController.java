package com.mido.pm.verify.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.verify.dto.SubjectTemplateDTO;
import com.mido.pm.verify.dto.SubjectTemplateVO;
import com.mido.pm.verify.service.NpssSubjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * NPSS 评价方式设置——租户级评价主体模板。整组提交（replace-all），保存即校验权重合计=100%、受益方≥50%。
 */
@RestController
@RequestMapping("/api/v1/npss/subject-templates")
public class NpssSubjectTemplateController {

    private final NpssSubjectService npssSubjectService;

    public NpssSubjectTemplateController(NpssSubjectService npssSubjectService) {
        this.npssSubjectService = npssSubjectService;
    }

    /** 租户评价主体模板列表。 */
    @GetMapping
    public R<List<SubjectTemplateVO>> list() {
        return R.ok(npssSubjectService.listTemplates());
    }

    /** 整组保存评价主体模板（replace-all）。 */
    @PutMapping
    public R<Void> save(@Valid @RequestBody List<SubjectTemplateDTO> items) {
        npssSubjectService.saveTemplates(items);
        return R.ok();
    }
}
