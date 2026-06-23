package com.mido.pm.task.dto;

import java.time.LocalDate;

/** 日历叠加用的轻量任务视图：当前用户可见、按截止日落点；isMilestone 用于里程碑标记。 */
public record CalendarTaskVO(
        Long id,
        Long projectId,
        String title,
        LocalDate dueDate,
        Integer isMilestone,
        String status) {
}
