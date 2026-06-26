package com.mido.pm.project.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.project.dto.ComponentVO;
import com.mido.pm.project.dto.ProjectComponentSaveDTO;
import com.mido.pm.project.dto.ProjectComponentVO;
import com.mido.pm.project.service.ComponentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 组件库 + 项目组件安装（动态顶栏）。 */
@RestController
@RequestMapping("/api/v1")
public class ComponentController {

    private final ComponentService componentService;

    public ComponentController(ComponentService componentService) {
        this.componentService = componentService;
    }

    /** 组件库目录。 */
    @GetMapping("/components")
    public R<List<ComponentVO>> catalog() {
        return R.ok(componentService.catalog());
    }

    /** 项目已安装组件。 */
    @GetMapping("/projects/{projectId}/components")
    public R<List<ProjectComponentVO>> listInstalled(@PathVariable Long projectId) {
        return R.ok(componentService.listInstalled(projectId));
    }

    /** 整列表保存（安装/卸载/排序）。 */
    @PutMapping("/projects/{projectId}/components")
    public R<Void> saveInstalled(@PathVariable Long projectId, @RequestBody List<ProjectComponentSaveDTO> components) {
        componentService.saveInstalled(projectId, components);
        return R.ok();
    }
}
