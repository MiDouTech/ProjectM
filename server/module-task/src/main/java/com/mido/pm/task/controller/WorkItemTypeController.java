package com.mido.pm.task.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.task.dto.TransitionDTO;
import com.mido.pm.task.dto.TypeFieldDTO;
import com.mido.pm.task.dto.WorkItemTypeSaveDTO;
import com.mido.pm.task.dto.WorkItemTypeVO;
import com.mido.pm.task.service.WorkItemTypeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 工作项类型 CRUD + 字段绑定 + 工作流转移矩阵（租户自配）。 */
@RestController
@RequestMapping("/api/v1/work-item-types")
public class WorkItemTypeController {

    private final WorkItemTypeService typeService;

    public WorkItemTypeController(WorkItemTypeService typeService) {
        this.typeService = typeService;
    }

    @GetMapping
    public R<List<WorkItemTypeVO>> list(@RequestParam(defaultValue = "false") boolean onlyActive) {
        return R.ok(typeService.list(onlyActive));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody WorkItemTypeSaveDTO dto) {
        return R.ok(typeService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody WorkItemTypeSaveDTO dto) {
        typeService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        typeService.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}/fields")
    public R<List<TypeFieldDTO>> getFields(@PathVariable Long id) {
        return R.ok(typeService.getFields(id));
    }

    @PutMapping("/{id}/fields")
    public R<Void> saveFields(@PathVariable Long id, @RequestBody List<TypeFieldDTO> fields) {
        typeService.saveFields(id, fields);
        return R.ok();
    }

    @GetMapping("/{id}/transitions")
    public R<List<TransitionDTO>> getTransitions(@PathVariable Long id) {
        return R.ok(typeService.getTransitions(id));
    }

    @PutMapping("/{id}/transitions")
    public R<Void> saveTransitions(@PathVariable Long id, @RequestBody List<TransitionDTO> transitions) {
        typeService.saveTransitions(id, transitions);
        return R.ok();
    }
}
