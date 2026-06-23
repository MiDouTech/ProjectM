package com.mido.pm.briefing.dto;

/** 模板字段定义：key 字段名、label 显示名、type 控件类型(text/textarea)。 */
public record FieldDefVO(
        String key,
        String label,
        String type) {
}
