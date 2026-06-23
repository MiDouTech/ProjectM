package com.mido.pm.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 新建日程。calendarId 为空时落到当前用户默认「我的日程」日历。
 * 时间合法性（end &gt; start）由 Service 校验。allDay 缺省 false，allowFeedback 缺省 true。
 */
public record ScheduleCreateDTO(
        Long calendarId,
        @NotBlank(message = "日程标题不能为空") String title,
        String description,
        @NotNull(message = "开始时间不能为空") LocalDateTime startTime,
        @NotNull(message = "结束时间不能为空") LocalDateTime endTime,
        Boolean allDay,
        String location,
        Boolean allowFeedback,
        List<ParticipantInputDTO> participants,
        List<Long> resourceIds,
        String recurRule) {
}
