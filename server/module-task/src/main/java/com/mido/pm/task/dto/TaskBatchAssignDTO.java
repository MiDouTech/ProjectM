package com.mido.pm.task.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 批量改负责人入参：assigneeId 为空表示批量取消指派。 */
public record TaskBatchAssignDTO(
        @NotEmpty(message = "请至少选择一条任务") List<Long> ids,
        Long assigneeId) {
}
