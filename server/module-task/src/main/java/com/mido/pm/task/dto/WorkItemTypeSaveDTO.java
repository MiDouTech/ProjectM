package com.mido.pm.task.dto;

import jakarta.validation.constraints.NotBlank;

/** 工作项类型创建/更新入参。code 创建后不可改。 */
public record WorkItemTypeSaveDTO(
        @NotBlank(message = "类型编码不能为空") String code,
        @NotBlank(message = "类型名不能为空") String name,
        String groupName,
        Integer sort,
        String status) {
}
