package com.mido.pm.view.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.view.service.TableColumnPrefService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 列表表头偏好（每用户每列表）：所有数据列表统一接入，配置跨设备一致（design-system 标准）。
 */
@RestController
@RequestMapping("/api/v1/table-prefs")
public class TableColumnPrefController {

    private final TableColumnPrefService service;

    public TableColumnPrefController(TableColumnPrefService service) {
        this.service = service;
    }

    /** 取当前用户某列表的表头偏好（未设置返回 null）。 */
    @GetMapping("/{listKey}")
    public R<Object> get(@PathVariable String listKey) {
        return R.ok(service.get(listKey));
    }

    /** 保存当前用户某列表的表头偏好（{columns:[...], frozen:[...]}）。 */
    @PutMapping("/{listKey}")
    public R<Void> save(@PathVariable String listKey, @RequestBody Map<String, Object> config) {
        service.save(listKey, config);
        return R.ok();
    }
}
