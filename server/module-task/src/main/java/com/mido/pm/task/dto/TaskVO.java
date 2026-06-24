package com.mido.pm.task.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record TaskVO(
        Long id,
        Long projectId,
        Long parentId,
        String title,
        String description,
        Long assigneeId,
        String status,
        Integer priority,
        String stage,
        LocalDate startDate,
        LocalDate dueDate,
        Integer isMilestone,
        LocalDateTime createTime,
        /** 自定义字段值 fieldKey→原始值，仅视图按 cf 列查询时填充；常规列表为 null */
        Map<String, String> customFields) {

    /** 常规构造（不含自定义字段）：customFields=null。 */
    public TaskVO(Long id, Long projectId, Long parentId, String title, String description,
                  Long assigneeId, String status, Integer priority, String stage,
                  LocalDate startDate, LocalDate dueDate, Integer isMilestone, LocalDateTime createTime) {
        this(id, projectId, parentId, title, description, assigneeId, status, priority, stage,
                startDate, dueDate, isMilestone, createTime, null);
    }

    /** 附加自定义字段值，返回新实例（不可变 record 的 wither）。 */
    public TaskVO withCustomFields(Map<String, String> cf) {
        return new TaskVO(id, projectId, parentId, title, description, assigneeId, status, priority,
                stage, startDate, dueDate, isMilestone, createTime, cf);
    }
}
