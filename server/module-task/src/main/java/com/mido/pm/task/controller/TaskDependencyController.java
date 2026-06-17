package com.mido.pm.task.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.task.dto.CriticalPathVO;
import com.mido.pm.task.dto.DependencyCreateDTO;
import com.mido.pm.task.dto.DependencyVO;
import com.mido.pm.task.service.TaskDependencyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 任务依赖：增删（新增带循环依赖检测）+ 项目依赖清单 + 关键路径。 */
@RestController
@RequestMapping("/api/v1/task-dependencies")
public class TaskDependencyController {

    private final TaskDependencyService dependencyService;

    public TaskDependencyController(TaskDependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }

    @PostMapping
    public R<Long> add(@Valid @RequestBody DependencyCreateDTO dto) {
        return R.ok(dependencyService.add(dto));
    }

    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        dependencyService.remove(id);
        return R.ok();
    }

    @GetMapping
    public R<List<DependencyVO>> listByProject(@RequestParam Long projectId) {
        return R.ok(dependencyService.listByProject(projectId));
    }

    /** 关键路径任务集（基于依赖 + 工期）。 */
    @GetMapping("/critical-path")
    public R<CriticalPathVO> criticalPath(@RequestParam Long projectId) {
        return R.ok(dependencyService.criticalPath(projectId));
    }
}
