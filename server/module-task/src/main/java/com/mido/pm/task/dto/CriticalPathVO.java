package com.mido.pm.task.dto;

import java.util.List;

/** 关键路径结果：关键任务 id 集与项目工期（最长路径长度，天）。 */
public record CriticalPathVO(List<Long> criticalTaskIds, long totalDurationDays) {
}
