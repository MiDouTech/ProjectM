package com.mido.pm.task.dto;

/** 任务依赖视图。 */
public record DependencyVO(Long id, Long predecessorId, Long successorId, String type) {
}
