package com.mido.pm.task.dto;

/** 任务查询。overdue=true 仅逾期(due_date<today 且未完成/未验收)；sort 形如 "dueDate,asc"。 */
public record TaskQueryDTO(
        Long page,
        Long size,
        Long projectId,
        Long assigneeId,
        String status,
        Boolean overdue,
        String sort) {
}
