package com.mido.pm.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TaskCreateDTO(
        @NotBlank(message = "任务标题不能为空") String title,
        @NotNull(message = "项目不能为空") Long projectId,
        Long parentId,
        Long assigneeId,
        Integer priority,
        String stage,
        LocalDate startDate,
        LocalDate dueDate,
        Integer isMilestone,
        String description,
        /** 循环规则（紧凑 JSON，见 TaskRecurrence）；非空则建任务后急切生成实例。 */
        String recurRule) {
}
