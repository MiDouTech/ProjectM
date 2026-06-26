package com.mido.pm.task.dto;

import jakarta.validation.constraints.NotNull;

/** 新建工作项关联入参（源任务取路径参数）。 */
public record RelationCreateDTO(
        @NotNull(message = "目标任务不能为空") Long targetTaskId,
        @NotNull(message = "关系类型不能为空") String relationKind) {
}
