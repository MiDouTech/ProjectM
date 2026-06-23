package com.mido.pm.calendar.service;

import com.mido.pm.calendar.dto.BusyVO;
import com.mido.pm.calendar.entity.PmSchedule;
import com.mido.pm.calendar.entity.PmScheduleParticipant;
import com.mido.pm.calendar.mapper.PmCalendarMapper;
import com.mido.pm.calendar.mapper.PmScheduleExceptionMapper;
import com.mido.pm.calendar.mapper.PmScheduleMapper;
import com.mido.pm.calendar.mapper.PmScheduleParticipantMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 成员忙闲单测：聚合参与日程为忙闲区间，仅含起止时间。 */
@ExtendWith(MockitoExtension.class)
class BusyServiceTest {

    @Mock private PmCalendarMapper calendarMapper;
    @Mock private PmScheduleParticipantMapper participantMapper;
    @Mock private PmScheduleMapper scheduleMapper;
    @Mock private PmScheduleExceptionMapper exceptionMapper;
    @InjectMocks private BusyService service;

    @Test
    void participantScheduleBecomesBusyInterval() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 23, 10, 0);
        LocalDateTime end = start.plusHours(1);
        when(calendarMapper.selectList(any())).thenReturn(List.of()); // 无名下日历
        PmScheduleParticipant part = new PmScheduleParticipant();
        part.setScheduleId(5L);
        part.setUserId(100L);
        when(participantMapper.selectList(any())).thenReturn(List.of(part));
        PmSchedule s = new PmSchedule();
        s.setId(5L);
        s.setTitle("机密评审");
        s.setStartTime(start);
        s.setEndTime(end);
        s.setAllDay(0);
        when(scheduleMapper.selectList(any())).thenReturn(List.of(s));

        List<BusyVO> busy = service.busyForUsers(List.of(100L),
                LocalDateTime.of(2026, 6, 23, 0, 0), LocalDateTime.of(2026, 6, 23, 23, 59));

        assertEquals(1, busy.size());
        assertEquals(100L, busy.get(0).userId());
        assertEquals(start, busy.get(0).start());
        assertEquals(end, busy.get(0).end());
    }

    @Test
    void noCalendarsNoParticipationYieldsEmpty() {
        when(calendarMapper.selectList(any())).thenReturn(List.of());
        when(participantMapper.selectList(any())).thenReturn(List.of());
        assertEquals(0, service.busyForUsers(List.of(100L),
                LocalDateTime.now(), LocalDateTime.now().plusDays(1)).size());
    }
}
