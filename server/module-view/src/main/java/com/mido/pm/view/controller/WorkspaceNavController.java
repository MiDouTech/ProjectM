package com.mido.pm.view.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.view.domain.WorkspaceCatalog.ComponentDef;
import com.mido.pm.view.dto.NavItemSaveDTO;
import com.mido.pm.view.dto.NavNodeVO;
import com.mido.pm.view.service.WorkspaceNavService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 可配置工作区导航（ADR-0003）：WorkspaceShell 取导航树；管理后台编排（catalog + 保存）。
 */
@RestController
@RequestMapping("/api/v1/workspace")
public class WorkspaceNavController {

    private final WorkspaceNavService service;

    public WorkspaceNavController(WorkspaceNavService service) {
        this.service = service;
    }

    /** 某一级模块的导航树（租户编排优先，空配置回落内置默认）。 */
    @GetMapping("/nav/{module}")
    public R<List<NavNodeVO>> nav(@PathVariable String module) {
        return R.ok(service.resolve(module));
    }

    /** 某模块可选组件（编排器用）。 */
    @GetMapping("/catalog/{module}")
    public R<List<ComponentDef>> catalog(@PathVariable String module) {
        return R.ok(service.catalog(module));
    }

    /** 原始编排（含停用项，编排器回显用；空=用默认）。 */
    @GetMapping("/nav/{module}/config")
    public R<List<NavItemSaveDTO>> rawConfig(@PathVariable String module) {
        return R.ok(service.rawConfig(module));
    }

    /** 整组保存某模块导航编排（管理后台）。 */
    @PutMapping("/nav/{module}")
    public R<Void> saveNav(@PathVariable String module, @RequestBody List<NavItemSaveDTO> items) {
        service.saveNav(module, items);
        return R.ok();
    }
}
