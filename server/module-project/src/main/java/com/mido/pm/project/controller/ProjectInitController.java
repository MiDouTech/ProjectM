package com.mido.pm.project.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.project.dto.InitiationFormDTO;
import com.mido.pm.project.service.ProjectInitService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 立项审批：提交立项申请，驱动项目进入审批流。 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectInitController {

    private final ProjectInitService initService;

    public ProjectInitController(ProjectInitService initService) {
        this.initService = initService;
    }

    /** 提交立项审批（api-conventions §1 动作型子路径）。返回审批实例 ID。 */
    @PostMapping("/{id}/submit-approval")
    public R<Long> submitApproval(@PathVariable Long id, @RequestBody InitiationFormDTO form) {
        return R.ok(initService.submitApproval(id, form));
    }
}
