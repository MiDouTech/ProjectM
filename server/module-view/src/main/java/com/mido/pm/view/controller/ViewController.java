package com.mido.pm.view.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.view.dto.ViewSaveDTO;
import com.mido.pm.view.dto.ViewVO;
import com.mido.pm.view.service.ViewService;
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

/** 视图设计器：命名视图 CRUD（个人/项目级）。任务的"按视图查询"在 module-task。 */
@RestController
@RequestMapping("/api/v1/views")
public class ViewController {

    private final ViewService viewService;

    public ViewController(ViewService viewService) {
        this.viewService = viewService;
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody ViewSaveDTO dto) {
        return R.ok(viewService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ViewSaveDTO dto) {
        viewService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        viewService.delete(id);
        return R.ok();
    }

    /** 当前项目可见视图：我的个人视图 ∪ 该项目的项目级视图。 */
    @GetMapping
    public R<List<ViewVO>> list(@RequestParam Long projectId) {
        return R.ok(viewService.listVisible(projectId));
    }

    @GetMapping("/{id}")
    public R<ViewVO> get(@PathVariable Long id) {
        return R.ok(viewService.get(id));
    }
}
