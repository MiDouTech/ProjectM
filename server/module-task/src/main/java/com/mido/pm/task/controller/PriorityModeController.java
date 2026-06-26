package com.mido.pm.task.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.task.dto.PriorityModeSaveDTO;
import com.mido.pm.task.dto.PriorityModeVO;
import com.mido.pm.task.service.PriorityModeService;
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

/** 优先级模式 CRUD（租户自配）。 */
@RestController
@RequestMapping("/api/v1/priority-modes")
public class PriorityModeController {

    private final PriorityModeService priorityModeService;

    public PriorityModeController(PriorityModeService priorityModeService) {
        this.priorityModeService = priorityModeService;
    }

    @GetMapping
    public R<List<PriorityModeVO>> list() {
        return R.ok(priorityModeService.list());
    }

    @GetMapping("/{id}")
    public R<PriorityModeVO> get(@PathVariable Long id) {
        return R.ok(priorityModeService.get(id));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody PriorityModeSaveDTO dto) {
        return R.ok(priorityModeService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PriorityModeSaveDTO dto) {
        priorityModeService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        priorityModeService.delete(id);
        return R.ok();
    }
}
