package com.mido.pm.calendar.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.calendar.dto.BusyVO;
import com.mido.pm.calendar.domain.RecurrenceExpander;
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

        List<BusyVO> busy = new ArrayList<>();
        for (PmSchedule s : schedules) {
            if (RecurrenceExpander.isRecurring(s)) {
                List<PmScheduleException> ex = exceptionMapper.selectList(
                        Wrappers.<PmScheduleException>lambdaQuery().eq(PmScheduleException::getScheduleId, s.getId()));
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
}
