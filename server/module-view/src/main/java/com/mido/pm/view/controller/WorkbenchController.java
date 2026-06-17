package com.mido.pm.view.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.view.dto.WorkbenchLayoutDTO;
import com.mido.pm.view.service.WorkbenchViewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 工作台布局：取/存当前用户的卡片有序列表（pm_view, scope=workbench）。 */
@RestController
@RequestMapping("/api/v1/workbench")
public class WorkbenchController {

    private final WorkbenchViewService workbenchViewService;

    public WorkbenchController(WorkbenchViewService workbenchViewService) {
        this.workbenchViewService = workbenchViewService;
    }

    @GetMapping("/layout")
    public R<WorkbenchLayoutDTO> getLayout() {
        return R.ok(workbenchViewService.getMyLayout());
    }

    @PutMapping("/layout")
    public R<Void> saveLayout(@RequestBody WorkbenchLayoutDTO dto) {
        workbenchViewService.saveMyLayout(dto);
        return R.ok();
    }
}
