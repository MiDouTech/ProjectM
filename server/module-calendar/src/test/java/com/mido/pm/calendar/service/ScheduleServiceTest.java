package com.mido.pm.calendar.service;

import com.mido.pm.calendar.dto.ParticipantInputDTO;
import com.mido.pm.calendar.dto.RsvpDTO;
import com.mido.pm.calendar.dto.ScheduleCreateDTO;
import com.mido.pm.calendar.dto.ScheduleUpdateDTO;
import com.mido.pm.calendar.entity.PmSchedule;
import com.mido.pm.calendar.entity.PmScheduleParticipant;
import com.mido.pm.calendar.event.CalendarEvents;
import com.mido.pm.calendar.mapper.PmScheduleMapper;
import com.mido.pm.calendar.mapper.PmScheduleParticipantMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 日程服务单测（mock mapper，无 DB）：
 * 时间区间校验、创建发 schedule.created 并自动落组织者参与人、RSVP 权限与状态校验、仅组织者可改。
 */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    private static final long ME = 100L;

    @Mock private PmScheduleMapper scheduleMapper;
    @Mock private PmScheduleParticipantMapper participantMapper;
    @Mock private CalendarService calendarService;
    @Mock private ResourceService resourceService;
    @Mock private DomainEventPublisher eventPublisher;
    @InjectMocks private ScheduleService service;

    @BeforeEach
    void setUp() {
        CurrentUser u = new CurrentUser();
        u.setUserId(ME);
        UserContext.set(u);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private ScheduleCreateDTO createDTO(LocalDateTime start, LocalDateTime end) {
        return new ScheduleCreateDTO(null, "周会", null, start, end,
                false, "会议室A", true, List.of(new ParticipantInputDTO(200L, null, "required")), null);
    }

    @Test
    void createRejectsWhenEndNotAfterStart() {
        LocalDateTime t = LocalDateTime.of(2026, 6, 23, 10, 0);
        assertThrows(BizException.class, () -> service.create(createDTO(t, t)));
        verify(scheduleMapper, never()).insert(any(PmSchedule.class));
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    void createInsertsScheduleOrganizerAndPublishesEvent() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 23, 10, 0);
        LocalDateTime end = start.plusHours(1);
        when(calendarService.resolveCalendarId(null)).thenReturn(10L);

        service.create(createDTO(start, end));

        verify(scheduleMapper).insert(any(PmSchedule.class));
        // 组织者 + 1 名受邀参与人 = 2 次 insert
        ArgumentCaptor<PmScheduleParticipant> pc = ArgumentCaptor.forClass(PmScheduleParticipant.class);
        verify(participantMapper, org.mockito.Mockito.times(2)).insert((PmScheduleParticipant) pc.capture());
        PmScheduleParticipant organizer = pc.getAllValues().get(0);
        assertEquals("organizer", organizer.getRole());
        assertEquals("accepted", organizer.getRsvpStatus());
        assertEquals(ME, organizer.getUserId());
        verify(eventPublisher).publish(eq(CalendarEvents.SCHEDULE_CREATED), any());
    }

    @Test
    void rsvpRejectsNonParticipant() {
        PmSchedule s = new PmSchedule();
        s.setAllowFeedback(1);
        when(scheduleMapper.selectById(5L)).thenReturn(s);
        when(participantMapper.selectOne(any())).thenReturn(null);

        assertThrows(BizException.class, () -> service.rsvp(5L, new RsvpDTO("accepted")));
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    void rsvpUpdatesStatusAndPublishes() {
        PmSchedule s = new PmSchedule();
        s.setAllowFeedback(1);
        when(scheduleMapper.selectById(5L)).thenReturn(s);
        PmScheduleParticipant p = new PmScheduleParticipant();
        p.setScheduleId(5L);
        p.setUserId(ME);
        p.setRsvpStatus("pending");
        when(participantMapper.selectOne(any())).thenReturn(p);

        service.rsvp(5L, new RsvpDTO("declined"));

        assertEquals("declined", p.getRsvpStatus());
        verify(participantMapper).updateById((PmScheduleParticipant) p);
        verify(eventPublisher).publish(eq(CalendarEvents.RSVP_RESPONDED), any());
    }

    @Test
    void rsvpRejectsIllegalStatus() {
        assertThrows(BizException.class, () -> service.rsvp(5L, new RsvpDTO("maybe")));
        verify(scheduleMapper, never()).selectById(any());
    }

    @Test
    void updateRejectsNonOrganizer() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 23, 10, 0);
        PmSchedule s = new PmSchedule();
        s.setOrganizerId(999L);
        when(scheduleMapper.selectById(5L)).thenReturn(s);

        ScheduleUpdateDTO dto = new ScheduleUpdateDTO("改标题", null, start, start.plusHours(1),
                false, null, true, null, null);
        assertThrows(BizException.class, () -> service.update(5L, dto));
        verify(scheduleMapper, never()).updateById(any(PmSchedule.class));
    }
}
