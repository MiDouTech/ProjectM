package com.mido.pm.task.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.task.dto.StatusSaveDTO;
import com.mido.pm.task.dto.StatusVO;
import com.mido.pm.task.service.StatusLibraryService;
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

/** 状态库 CRUD（租户自配）。 */
@RestController
@RequestMapping("/api/v1/statuses")
public class StatusController {

    private final StatusLibraryService statusService;

    public StatusController(StatusLibraryService statusService) {
        this.statusService = statusService;
    }

    @GetMapping
    public R<List<StatusVO>> list(@RequestParam(defaultValue = "false") boolean onlyActive) {
        return R.ok(statusService.list(onlyActive));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody StatusSaveDTO dto) {
        return R.ok(statusService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody StatusSaveDTO dto) {
        statusService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        statusService.delete(id);
        return R.ok();
    }
}
