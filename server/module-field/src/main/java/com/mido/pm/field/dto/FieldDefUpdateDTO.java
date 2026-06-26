package com.mido.pm.field.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 修改字段定义。scope/fieldKey 不可改（改标识等同新建字段，避免历史值错位）。
 * type 可改但会影响存量值解释，由调用方自负；enabled 控制启停。
 */
public record FieldDefUpdateDTO(
        @NotBlank(message = "字段名不能为空") String name,
        @NotBlank(message = "字段类型不能为空") String type,
        @Valid List<FieldOption> options,
        Long dataSourceId,
        Boolean required,
        Integer sortNo,
        Boolean enabled) {
}
