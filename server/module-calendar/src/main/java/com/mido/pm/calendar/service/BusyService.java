package com.mido.pm.calendar.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.calendar.dto.BusyVO;
import com.mido.pm.calendar.dto.FindSlotsDTO;
import com.mido.pm.calendar.dto.SlotVO;
import com.mido.pm.calendar.domain.RecurrenceExpander;
import com.mido.pm.calendar.domain.SlotFinder;
import com.mido.pm.calendar.entity.PmCalendar;
import com.mido.pm.calendar.entity.PmSchedule;
import com.mido.pm.calendar.entity.PmScheduleException;
import com.mido.pm.calendar.entity.PmScheduleParticipant;
import com.mido.pm.calendar.mapper.PmCalendarMapper;
import com.mido.pm.calendar.mapper.PmScheduleExceptionMapper;
import com.mido.pm.calendar.mapper.PmScheduleMapper;
import com.mido.pm.calendar.mapper.PmScheduleParticipantMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

/**
 * 成员忙闲服务：聚合某用户在 [from,to] 的 confirmed 日程占用区间（其名下日历 + 其参与的日程，循环已展开），
 * 仅输出起止时间、不暴露标题/详情。供「成员忙闲」叠加与「排期小助手」找空档复用。
 */
@Service
public class BusyService {

    private final PmCalendarMapper calendarMapper;
    private final PmScheduleParticipantMapper participantMapper;
    private final PmScheduleMapper scheduleMapper;
    private final PmScheduleExceptionMapper exceptionMapper;

    public BusyService(PmCalendarMapper calendarMapper,
                       PmScheduleParticipantMapper participantMapper,
                       PmScheduleMapper scheduleMapper,
                       PmScheduleExceptionMapper exceptionMapper) {
        this.calendarMapper = calendarMapper;
        this.participantMapper = participantMapper;
        this.scheduleMapper = scheduleMapper;
        this.exceptionMapper = exceptionMapper;
    }

    /** 排期小助手：在 date 的工作时段内，找所有参选人皆空闲且时长足够的空档。 */
    public List<SlotVO> findSlots(FindSlotsDTO dto) {
        LocalTime dayStart = parseTime(dto.dayStart(), LocalTime.of(9, 0));
        LocalTime dayEnd = parseTime(dto.dayEnd(), LocalTime.of(18, 0));
        LocalDateTime from = dto.date().atTime(dayStart);
        LocalDateTime to = dto.date().atTime(dayEnd);
        List<SlotFinder.Interval> busy = busyForUsers(dto.userIds(), from, to).stream()
                .map(b -> new SlotFinder.Interval(b.start(), b.end())).toList();
        return SlotFinder.freeSlots(busy, from, to, dto.durationMinutes()).stream()
                .map(i -> new SlotVO(i.start(), i.end())).toList();
    }

    private LocalTime parseTime(String hhmm, LocalTime fallback) {
        if (hhmm == null || hhmm.isBlank()) {
            return fallback;
        }
        try {
            return LocalTime.parse(hhmm);
        } catch (Exception e) {
            return fallback;
        }
    }

    /** 多个用户在 [from,to] 的忙闲区间（每段一条）。 */
    public List<BusyVO> busyForUsers(List<Long> userIds, LocalDateTime from, LocalDateTime to) {
        List<BusyVO> result = new ArrayList<>();
        if (userIds == null) {
            return result;
        }
        for (Long uid : userIds.stream().distinct().toList()) {
            result.addAll(busyForUser(uid, from, to));
        }
        return result;
    }

    private List<BusyVO> busyForUser(Long uid, LocalDateTime from, LocalDateTime to) {
        List<Long> calIds = calendarMapper.selectList(Wrappers.<PmCalendar>lambdaQuery()
                        .eq(PmCalendar::getOwnerId, uid))
                .stream().map(PmCalendar::getId).toList();
        List<Long> partIds = participantMapper.selectList(Wrappers.<PmScheduleParticipant>lambdaQuery()
                        .eq(PmScheduleParticipant::getUserId, uid))
                .stream().map(PmScheduleParticipant::getScheduleId).toList();
        if (calIds.isEmpty() && partIds.isEmpty()) {
            return List.of();
        }
        List<Long> cals = calIds.isEmpty() ? List.of(-1L) : calIds;
        List<PmSchedule> schedules = scheduleMapper.selectList(Wrappers.<PmSchedule>lambdaQuery()
                .and(w -> {
                    w.in(PmSchedule::getCalendarId, cals);
                    if (!partIds.isEmpty()) {
                        w.or().in(PmSchedule::getId, partIds);
                    }
                })
                .eq(PmSchedule::getStatus, "confirmed"));

        // 批量加载循环日程例外，避免每个循环日程一次查询(N+1)
        Map<Long, List<PmScheduleException>> exBySchedule = exceptionsBySchedule(schedules.stream()
                .filter(RecurrenceExpander::isRecurring).map(PmSchedule::getId).toList());
        List<BusyVO> busy = new ArrayList<>();
        for (PmSchedule s : schedules) {
            if (RecurrenceExpander.isRecurring(s)) {
                List<PmScheduleException> ex = exBySchedule.getOrDefault(s.getId(), List.of());
                for (RecurrenceExpander.Occurrence o : RecurrenceExpander.expand(s, ex, from, to)) {
                    busy.add(new BusyVO(uid, o.start(), o.end(), s.getAllDay()));
                }
            } else if (overlaps(s.getStartTime(), s.getEndTime(), from, to)) {
                busy.add(new BusyVO(uid, s.getStartTime(), s.getEndTime(), s.getAllDay()));
            }
        }
        return busy;
    }

    private boolean overlaps(LocalDateTime s, LocalDateTime e, LocalDateTime from, LocalDateTime to) {
        return s.isBefore(to) && e.isAfter(from);
    }

    /** 一次性按 scheduleId 批量加载例外并分组（避免 N+1）。 */
    private Map<Long, List<PmScheduleException>> exceptionsBySchedule(List<Long> scheduleIds) {
        if (scheduleIds.isEmpty()) {
            return Map.of();
        }
        return exceptionMapper.selectList(Wrappers.<PmScheduleException>lambdaQuery()
                        .in(PmScheduleException::getScheduleId, scheduleIds))
                .stream().collect(Collectors.groupingBy(PmScheduleException::getScheduleId));
    }
}
