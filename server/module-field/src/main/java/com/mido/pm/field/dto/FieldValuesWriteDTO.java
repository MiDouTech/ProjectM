package com.mido.pm.field.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量写入某实体的字段值。entityType=task/project；
 * values 为 (fieldId, value) 列表，缺省字段不变更，value 为空表示清除该字段。
 */
public record FieldValuesWriteDTO(
        @NotNull(message = "实体类型不能为空") String entityType,
        @NotNull(message = "实体 id 不能为空") Long entityId,
        @NotEmpty(message = "至少提交一个字段值") @Valid List<Item> values) {

    public record Item(
            @NotNull(message = "字段 id 不能为空") Long fieldId,
            String value) {
    }
}
