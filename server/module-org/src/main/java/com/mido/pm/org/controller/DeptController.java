package com.mido.pm.org.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.org.dto.DeptCreateDTO;
import com.mido.pm.org.dto.DeptUpdateDTO;
import com.mido.pm.org.dto.DeptVO;
import com.mido.pm.org.service.SysDeptService;
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

/** 部门树 CRUD。 */
@RestController
@RequestMapping("/api/v1/depts")
public class DeptController {

    private final SysDeptService deptService;

    public DeptController(SysDeptService deptService) {
        this.deptService = deptService;
    }

    @GetMapping
    public R<List<DeptVO>> tree() {
        return R.ok(deptService.tree());
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody DeptCreateDTO dto) {
        return R.ok(deptService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody DeptUpdateDTO dto) {
        deptService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return R.ok();
    }
}
