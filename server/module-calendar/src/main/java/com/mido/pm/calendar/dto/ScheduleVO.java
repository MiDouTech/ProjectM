package com.mido.pm.calendar.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 日程视图。participants 在详情接口返回，列表接口可为空。 */
public record ScheduleVO(
        Long id,
        Long calendarId,
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer allDay,
        String location,
        Integer allowFeedback,
        String sourceType,
        Long sourceId,
        Long organizerId,
        String status,
        List<ParticipantVO> participants,
        List<Long> resourceIds) {
}
