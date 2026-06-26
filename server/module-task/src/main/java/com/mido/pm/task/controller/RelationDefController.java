package com.mido.pm.task.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.task.dto.RelationDefSaveDTO;
import com.mido.pm.task.dto.RelationDefVO;
import com.mido.pm.task.service.RelationDefService;
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

/** 关联定义 CRUD（租户自配：类型↔类型 的关联语义）。 */
@RestController
@RequestMapping("/api/v1/relation-defs")
public class RelationDefController {

    private final RelationDefService relationDefService;

    public RelationDefController(RelationDefService relationDefService) {
        this.relationDefService = relationDefService;
    }

    @GetMapping
    public R<List<RelationDefVO>> list() {
        return R.ok(relationDefService.list());
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody RelationDefSaveDTO dto) {
        return R.ok(relationDefService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RelationDefSaveDTO dto) {
        relationDefService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        relationDefService.delete(id);
        return R.ok();
    }
}
