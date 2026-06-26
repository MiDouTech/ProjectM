package com.mido.pm.task.dto;

import jakarta.validation.constraints.NotNull;

/** 关联定义创建/更新入参。 */
public record RelationDefSaveDTO(
        @NotNull(message = "源类型不能为空") Long sourceTypeId,
        @NotNull(message = "目标类型不能为空") Long targetTypeId,
        @NotNull(message = "关系类型不能为空") String relationKind,
        String name) {
}
