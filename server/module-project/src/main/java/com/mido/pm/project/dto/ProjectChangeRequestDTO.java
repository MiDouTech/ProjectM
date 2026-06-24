package com.mido.pm.project.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * 项目时间（计划基线）变更发起请求。拟改值仅填需变更项（null=不改）。
 * 对标 Worktile「项目时间变更」：受控变更，走变更中心 + 审批引擎。
 */
public record ProjectChangeRequestDTO(
        @NotBlank(message = "变更类型必填") String changeType,
        @NotBlank(message = "变更事由必填") String reason,
        String impact,
        LocalDate startDate,
        LocalDate endDate) {
}
