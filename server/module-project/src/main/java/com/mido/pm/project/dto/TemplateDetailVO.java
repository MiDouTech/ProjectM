package com.mido.pm.project.dto;

/** 项目模板详情（含 config，编辑回显用）。 */
public record TemplateDetailVO(
        Long id,
        String name,
        String category,
        String subCategory,
        String description,
        Integer isBuiltin,
        String config) {
}
