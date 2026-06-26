package com.mido.pm.task.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 状态库创建/更新入参。metaCategory 必须为 未开始/进行中/已完成 之一。
 */
public record StatusSaveDTO(
        @NotBlank(message = "状态名不能为空") String name,
        String color,
        @NotBlank(message = "元类别不能为空") String metaCategory,
        String groupName,
        Integer sort,
        String status) {
}
