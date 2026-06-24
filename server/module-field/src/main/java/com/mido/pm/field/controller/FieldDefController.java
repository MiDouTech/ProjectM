package com.mido.pm.field.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.field.dto.FieldDefCreateDTO;
import com.mido.pm.field.dto.FieldDefUpdateDTO;
import com.mido.pm.field.dto.FieldDefVO;
import com.mido.pm.field.service.FieldDefService;
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

/** 自定义字段定义：按作用域(task/project)增删改查，供管理配置页(P2-4b)使用。 */
@RestController
@RequestMapping("/api/v1/field-defs")
public class FieldDefController {

    private final FieldDefService fieldDefService;

    public FieldDefController(FieldDefService fieldDefService) {
        this.fieldDefService = fieldDefService;
    }

    /** 列出某作用域字段定义；enabledOnly=true 仅返回启用项。 */
    @GetMapping
    public R<List<FieldDefVO>> list(@RequestParam String scope,
                                    @RequestParam(defaultValue = "false") boolean enabledOnly) {
        return R.ok(fieldDefService.list(scope, enabledOnly));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody FieldDefCreateDTO dto) {
        return R.ok(fieldDefService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody FieldDefUpdateDTO dto) {
        fieldDefService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        fieldDefService.delete(id);
        return R.ok();
    }
}
