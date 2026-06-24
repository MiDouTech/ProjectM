package com.mido.pm.project.controller;

import com.mido.pm.change.dto.ChangeRequestVO;
import com.mido.pm.common.api.R;
import com.mido.pm.project.dto.ProjectChangeRequestDTO;
import com.mido.pm.project.service.ProjectChangeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 项目时间变更：发起变更（受控，复用审批引擎）+ 查看本项目变更历史。台账总览见 /api/v1/changes。 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/changes")
public class ProjectChangeController {

    private final ProjectChangeService projectChangeService;

    public ProjectChangeController(ProjectChangeService projectChangeService) {
        this.projectChangeService = projectChangeService;
    }

    /** 发起项目时间变更，返回变更单 id。 */
    @PostMapping
    public R<Long> submit(@PathVariable Long projectId, @Valid @RequestBody ProjectChangeRequestDTO dto) {
        return R.ok(projectChangeService.submit(projectId, dto));
    }

    /** 本项目的变更历史。 */
    @GetMapping
    public R<List<ChangeRequestVO>> list(@PathVariable Long projectId) {
        return R.ok(projectChangeService.list(projectId));
    }
}
