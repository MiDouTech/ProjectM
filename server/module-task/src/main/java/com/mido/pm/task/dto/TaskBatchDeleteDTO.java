package com.mido.pm.task.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 批量删除（逻辑删）入参。 */
public record TaskBatchDeleteDTO(
        @NotEmpty(message = "请至少选择一条任务") List<Long> ids) {
}
