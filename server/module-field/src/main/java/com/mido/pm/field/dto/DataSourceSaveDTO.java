package com.mido.pm.field.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** 数据源创建/更新入参。options 为选项集（先清后插）。 */
public record DataSourceSaveDTO(
        @NotBlank(message = "数据源名称不能为空") String name,
        String groupName,
        String remark,
        String status,
        @Valid List<FieldOption> options) {
}
