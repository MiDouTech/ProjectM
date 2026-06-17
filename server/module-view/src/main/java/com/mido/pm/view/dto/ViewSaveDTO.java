package com.mido.pm.view.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 保存视图入参。scope=personal|project；type=kanban|list|table|gantt|calendar；project 级须带 projectId。 */
public record ViewSaveDTO(
        @NotBlank(message = "视图名不能为空") String name,
        @NotBlank(message = "视图范围不能为空") String scope,
        @NotBlank(message = "视图类型不能为空") String type,
        Long projectId,
        @NotNull(message = "视图配置不能为空") ViewConfig config) {
}
