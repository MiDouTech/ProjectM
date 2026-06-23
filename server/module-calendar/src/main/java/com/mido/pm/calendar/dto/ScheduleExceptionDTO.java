package com.mido.pm.calendar.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 循环日程例外入参。action=cancel 取消该次 / modify 改期；
 * modify 时 override 传覆盖内容（startTime/endTime/title/location，均可选）。
 */
public record ScheduleExceptionDTO(
        @NotNull(message = "实例日期不能为空") LocalDate occurDate,
        String action,
        OverrideDTO override) {

    /** 改期覆盖内容（ISO-8601 时间）。 */
    public record OverrideDTO(
            String startTime,
            String endTime,
            String title,
            String location) {
    }
}
