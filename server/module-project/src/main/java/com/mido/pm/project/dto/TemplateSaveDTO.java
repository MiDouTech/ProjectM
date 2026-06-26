package com.mido.pm.project.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 项目模板创建/更新入参。config 为阶段/任务骨架/默认干系人权重/默认审批流/默认字段的 JSON 文本（可空）。
 * 内置模板（is_builtin=1）禁止编辑/删除，仅自定义模板可写。
 */
public record TemplateSaveDTO(
        @NotBlank(message = "模板名称不能为空") String name,
        @NotBlank(message = "项目类型不能为空") String category,
        String subCategory,
        String description,
        String config) {
}
