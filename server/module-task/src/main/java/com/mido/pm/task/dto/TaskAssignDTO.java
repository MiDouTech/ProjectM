package com.mido.pm.task.dto;

/** 指派/改派；assigneeId 为 null 表示取消指派。 */
public record TaskAssignDTO(Long assigneeId) {
}
