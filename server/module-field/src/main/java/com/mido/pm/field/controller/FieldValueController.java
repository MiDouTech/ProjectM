package com.mido.pm.field.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.field.dto.FieldValueVO;
import com.mido.pm.field.dto.FieldValuesWriteDTO;
import com.mido.pm.field.service.FieldValueService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 自定义字段值：按实体(entityType=task/project + entityId)读取「定义+值」、批量写入。
 * 供任务/项目详情右抽屉渲染与编辑。
 */
@RestController
@RequestMapping("/api/v1/field-values")
public class FieldValueController {

    private final FieldValueService fieldValueService;

    public FieldValueController(FieldValueService fieldValueService) {
        this.fieldValueService = fieldValueService;
    }

    /** 读取实体的字段定义+当前值（含尚无值的启用字段）。 */
    @GetMapping
    public R<List<FieldValueVO>> list(@RequestParam String entityType, @RequestParam Long entityId) {
        return R.ok(fieldValueService.getValues(entityType, entityId));
    }

    /** 批量写入字段值（仅处理提交字段，value 空表示清除）。 */
    @PutMapping
    public R<Void> save(@Valid @RequestBody FieldValuesWriteDTO dto) {
        fieldValueService.saveValues(dto);
        return R.ok();
    }
}
