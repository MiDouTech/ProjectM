package com.mido.pm.field.dto;

import java.util.List;

/**
 * 字段值视图（详情渲染用）：字段定义元信息 + 当前值。
 * value 为入库原始字符串（多选/用户型为 JSON 数组字符串），由前端按 type 解析渲染。
 */
public record FieldValueVO(
        Long fieldId,
        String fieldKey,
        String name,
        String type,
        List<FieldOption> options,
        boolean required,
        Integer sortNo,
        String value) {
}
