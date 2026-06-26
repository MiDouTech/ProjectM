package com.mido.pm.verify.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.verify.dto.ProjectSubjectDTO;
import com.mido.pm.verify.dto.ProjectSubjectVO;
import com.mido.pm.verify.service.NpssSubjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * NPSS 评价方式设置——项目级评价主体（成员即干系人）。未配置时 GET 返回按启用模板派生的草稿，供前端预填。
 * 整组提交（replace-all），保存即校验权重合计=100%、受益方≥50%、每个主体≥1 成员。
 */
@RestController
@RequestMapping("/api/v1/npss/projects/{projectId}/subjects")
public class NpssProjectSubjectController {

    private final NpssSubjectService npssSubjectService;

    public NpssProjectSubjectController(NpssSubjectService npssSubjectService) {
        this.npssSubjectService = npssSubjectService;
    }

    /** 项目评价主体（含成员）；未配置则返回模板派生草稿。 */
    @GetMapping
    public R<List<ProjectSubjectVO>> list(@PathVariable Long projectId) {
        return R.ok(npssSubjectService.listProjectSubjects(projectId));
    }

    /** 整组保存项目评价主体（replace-all）。 */
    @PutMapping
    public R<Void> save(@PathVariable Long projectId, @Valid @RequestBody List<ProjectSubjectDTO> items) {
        npssSubjectService.saveProjectSubjects(projectId, items);
        return R.ok();
    }
}
