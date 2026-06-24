package com.mido.pm.field.dto;

import java.util.List;

/** 字段定义视图。 */
public record FieldDefVO(
        Long id,
        String scope,
        String fieldKey,
        String name,
        String type,
        List<FieldOption> options,
        boolean required,
        Integer sortNo,
        boolean enabled) {
}
