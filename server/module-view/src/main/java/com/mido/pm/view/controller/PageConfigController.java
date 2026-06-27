package com.mido.pm.view.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.view.domain.PageFieldCatalog.FieldDef;
import com.mido.pm.view.service.PageConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 可配置页面表单（ADR-0004 · L3）：内置字段目录 + 页面配置读写。
 * 前端将内置字段与 pm_field_def 自定义字段合成为统一字段集供编排。
 */
@RestController
@RequestMapping("/api/v1/workspace/page")
public class PageConfigController {

    private final PageConfigService service;

    public PageConfigController(PageConfigService service) {
        this.service = service;
    }

    /** 某实体的内置字段目录。 */
    @GetMapping("/fields/{target}")
    public R<List<FieldDef>> fields(@PathVariable String target) {
        return R.ok(service.builtinFields(target));
    }

    /** 某实体某模板的页面配置（空=回落默认）。 */
    @GetMapping("/{target}/{templateType}")
    public R<Object> get(@PathVariable String target, @PathVariable String templateType) {
        return R.ok(service.get(target, templateType));
    }

    /** 保存页面配置（租户级配置，限配置管理权限）。 */
    @PreAuthorize("hasAuthority('org:config:manage')")
    @PutMapping("/{target}/{templateType}")
    public R<Void> save(@PathVariable String target, @PathVariable String templateType,
                        @RequestBody Map<String, Object> config) {
        service.save(target, templateType, config);
        return R.ok();
    }
}
