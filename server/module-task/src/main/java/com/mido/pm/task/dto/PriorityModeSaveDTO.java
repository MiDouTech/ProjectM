package com.mido.pm.task.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** 优先级模式创建/更新入参。levels 为档位集（先清后插）。 */
public record PriorityModeSaveDTO(
        @NotBlank(message = "模式名不能为空") String name,
        String remark,
        String status,
        List<PriorityLevelDTO> levels) {
}
