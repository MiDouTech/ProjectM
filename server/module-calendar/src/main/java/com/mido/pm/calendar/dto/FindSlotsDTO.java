package com.mido.pm.calendar.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * 排期小助手入参。dayStart/dayEnd 为 "HH:mm"，缺省 09:00–18:00。
 */
public record FindSlotsDTO(
        @NotNull(message = "参选人不能为空") List<Long> userIds,
        @NotNull(message = "日期不能为空") LocalDate date,
        @NotNull(message = "会议时长不能为空") Integer durationMinutes,
        String dayStart,
        String dayEnd) {
}
