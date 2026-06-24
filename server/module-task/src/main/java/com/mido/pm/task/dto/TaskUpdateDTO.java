package com.mido.pm.task.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record TaskUpdateDTO(
        @NotBlank(message = "任务标题不能为空") String title,
        Integer priority,
        String stage,
        LocalDate startDate,
        LocalDate dueDate,
        Integer isMilestone,
        String description,
        /** 循环规则（紧凑 JSON，见 TaskRecurrence）；改规则后可调用生成端点补齐实例。 */
        String recurRule) {
}
