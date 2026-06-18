package com.mido.pm.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 批量改状态入参：目标状态须经工作流合法性校验。 */
public record TaskBatchStatusDTO(
        @NotEmpty(message = "请至少选择一条任务") List<Long> ids,
        @NotBlank(message = "目标状态不能为空") String targetStatus) {
}
