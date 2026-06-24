package com.mido.pm.report.dto;

/**
 * 人员负荷项：某负责人名下未完成（进行中）任务数与其中逾期数。
 * assigneeId 由前端按成员字典解析为姓名（与项目/任务列表口径一致）。
 */
public record WorkloadItemVO(Long assigneeId, long inProgress, long overdue) {
}
