package com.mido.pm.goal.domain;

import java.math.BigDecimal;

/**
 * 项目完成率端口（SPI）：目标进度自动汇总所需的"某项目任务完成率"，由任务域（module-task）实现并注入。
 * 目标引擎不直接依赖任务表，跨域只经接口（CLAUDE.md §4）。口径：已完成任务/总任务，返回 0–100。
 */
public interface ProjectCompletionPort {

    /** 项目任务完成率（0–100）；无任务返回 0。 */
    BigDecimal completionRate(Long projectId);
}
