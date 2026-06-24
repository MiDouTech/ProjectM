package com.mido.pm.task.controller;

import com.mido.pm.change.dto.ChangeRequestVO;
import com.mido.pm.common.api.R;
import com.mido.pm.task.dto.TaskChangeRequestDTO;
import com.mido.pm.task.service.TaskChangeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 重大任务变更：发起变更（受控，复用审批引擎）+ 查看本任务变更历史。台账总览见 /api/v1/changes。 */
@RestController
@RequestMapping("/api/v1/tasks/{taskId}/changes")
public class TaskChangeController {

    private final TaskChangeService taskChangeService;

    public TaskChangeController(TaskChangeService taskChangeService) {
        this.taskChangeService = taskChangeService;
    }

    /** 发起重大任务变更，返回变更单 id。 */
    @PostMapping
    public R<Long> submit(@PathVariable Long taskId, @Valid @RequestBody TaskChangeRequestDTO dto) {
        return R.ok(taskChangeService.submit(taskId, dto));
    }

    /** 本任务的变更历史。 */
    @GetMapping
    public R<List<ChangeRequestVO>> list(@PathVariable Long taskId) {
        return R.ok(taskChangeService.list(taskId));
    }
}
