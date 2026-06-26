package com.mido.pm.task.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.task.dto.RelationCreateDTO;
import com.mido.pm.task.dto.TaskRelationVO;
import com.mido.pm.task.service.RelationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 工作项关联（追溯）：任务的 related/derived 关联增删查。 */
@RestController
@RequestMapping("/api/v1/tasks/{taskId}/relations")
public class RelationController {

    private final RelationService relationService;

    public RelationController(RelationService relationService) {
        this.relationService = relationService;
    }

    @GetMapping
    public R<List<TaskRelationVO>> list(@PathVariable Long taskId) {
        return R.ok(relationService.listForTask(taskId));
    }

    @PostMapping
    public R<Long> add(@PathVariable Long taskId, @Valid @RequestBody RelationCreateDTO dto) {
        return R.ok(relationService.link(taskId, dto));
    }

    @DeleteMapping("/{relationId}")
    public R<Void> remove(@PathVariable Long taskId, @PathVariable Long relationId) {
        relationService.unlink(taskId, relationId);
        return R.ok();
    }
}
