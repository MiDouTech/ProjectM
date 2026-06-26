package com.mido.pm.field.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 新建字段定义。scope=task/project；type 见 FieldType；
 * select/multi_select 须给 options。fieldKey 在(租户,scope)内唯一。
 */
public record FieldDefCreateDTO(
        @NotBlank(message = "作用域不能为空") String scope,
        @NotBlank(message = "字段标识不能为空") String fieldKey,
        @NotBlank(message = "字段名不能为空") String name,
        @NotBlank(message = "字段类型不能为空") String type,
        @Valid List<FieldOption> options,
        Long dataSourceId,
        Boolean required,
        Integer sortNo) {
}
