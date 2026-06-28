package com.mido.pm.verify.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.verify.dto.SubjectTemplateDTO;
import com.mido.pm.verify.dto.SubjectTemplateVO;
import com.mido.pm.verify.service.NpssSubjectService;
import com.mido.pm.verify.service.NpssSubjectTemplateChangeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * NPSS 评价方式设置——租户级评价主体模板。整组提交（replace-all），保存即校验权重合计=100%、受益方≥50%。
 * 修改经变更中心受控落库（默认免审即时生效、留痕；配置变更策略后自动改走审批）。
 */
@RestController
@RequestMapping("/api/v1/npss/subject-templates")
public class NpssSubjectTemplateController {

    private final NpssSubjectService npssSubjectService;
    private final NpssSubjectTemplateChangeService templateChangeService;

    public NpssSubjectTemplateController(NpssSubjectService npssSubjectService,
                                         NpssSubjectTemplateChangeService templateChangeService) {
        this.npssSubjectService = npssSubjectService;
        this.templateChangeService = templateChangeService;
    }

    /** 租户评价主体模板列表。 */
    @GetMapping
    public R<List<SubjectTemplateVO>> list() {
        return R.ok(npssSubjectService.listTemplates());
    }

    /** 整组保存评价主体模板（replace-all）：经变更中心受控落库。 */
    @PutMapping
    public R<Void> save(@Valid @RequestBody List<SubjectTemplateDTO> items) {
        templateChangeService.submit(items);
        return R.ok();
    }
}
