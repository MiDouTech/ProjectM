package com.mido.pm.task.dto;

/** 工作流转移项（from_status → to_status）。 */
public record TransitionDTO(Long fromStatusId, Long toStatusId) {
}
