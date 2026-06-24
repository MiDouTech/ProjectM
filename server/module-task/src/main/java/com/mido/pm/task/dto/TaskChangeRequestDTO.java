package com.mido.pm.task.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * 重大任务（基线）变更发起请求。拟改值仅填需变更项（null=不改）。
 * 对标 Worktile「重大任务变更」：受控变更，走变更中心 + 审批引擎。
 */
public record TaskChangeRequestDTO(
        @NotBlank(message = "变更类型必填") String changeType,
        @NotBlank(message = "变更事由必填") String reason,
        String impact,
        LocalDate startDate,
        LocalDate dueDate,
        Long assigneeId) {
}
