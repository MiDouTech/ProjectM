package com.mido.pm.task.dto;

import jakarta.validation.constraints.NotNull;

/** 新增依赖入参。type 不传默认 FS。 */
public record DependencyCreateDTO(
        @NotNull(message = "前置任务不能为空") Long predecessorId,
        @NotNull(message = "后继任务不能为空") Long successorId,
        String type) {
}
