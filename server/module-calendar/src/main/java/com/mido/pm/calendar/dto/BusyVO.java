package com.mido.pm.calendar.dto;

import java.time.LocalDateTime;

/** 成员忙闲区间：只暴露归属用户与起止时间，不含日程标题/详情。 */
public record BusyVO(
        Long userId,
        LocalDateTime start,
        LocalDateTime end,
        Integer allDay) {
}
