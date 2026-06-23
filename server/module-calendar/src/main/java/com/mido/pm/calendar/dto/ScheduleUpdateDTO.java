package com.mido.pm.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 更新日程。participants 非空时整体覆盖参与人（保留各自已有 RSVP 状态，新增者为 pending）。
 */
public record ScheduleUpdateDTO(
        @NotBlank(message = "日程标题不能为空") String title,
        String description,
        @NotNull(message = "开始时间不能为空") LocalDateTime startTime,
        @NotNull(message = "结束时间不能为空") LocalDateTime endTime,
        Boolean allDay,
        String location,
        Boolean allowFeedback,
        List<ParticipantInputDTO> participants,
        List<Long> resourceIds,
        String recurRule,
        List<Integer> reminderMinutes) {
}
