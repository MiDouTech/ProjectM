package com.mido.pm.calendar.job;

import com.mido.pm.calendar.entity.PmSchedule;
import com.mido.pm.calendar.entity.PmScheduleReminderLog;
import com.mido.pm.calendar.event.CalendarEvents;
import com.mido.pm.calendar.mapper.PmScheduleMapper;
import com.mido.pm.calendar.mapper.PmScheduleReminderLogMapper;
import com.mido.pm.common.outbox.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 提醒扫描单测：到点未发→发事件并记日志；已记录→不重发；未到点→不发。 */
@ExtendWith(MockitoExtension.class)
class ReminderScanJobTest {

    @Mock private PmScheduleMapper scheduleMapper;
    @Mock private PmScheduleReminderLogMapper reminderLogMapper;
    @Mock private DomainEventPublisher eventPublisher;
    @InjectMocks private ReminderScanJob job;

    private PmSchedule schedule(LocalDateTime start, String reminder) {
        PmSchedule s = new PmSchedule();
        s.setId(1L);
        s.setTitle("评审会");
        s.setStartTime(start);
        s.setOrganizerId(100L);
        s.setReminder(reminder);
        return s;
    }

    @Test
    void dueReminderPublishesOnceAndLogs() {
        // start 在 10 分钟后，提前 15 分钟提醒 → 提醒点已过
        PmSchedule s = schedule(LocalDateTime.now().plusMinutes(10), "[15]");
        when(reminderLogMapper.selectCount(any())).thenReturn(0L);

        job.dispatch(s, LocalDateTime.now());

        verify(eventPublisher).publish(eq(CalendarEvents.REMINDER_DUE), any());
        verify(reminderLogMapper).insert(any(PmScheduleReminderLog.class));
    }

    @Test
    void alreadySentDoesNotRepublish() {
        PmSchedule s = schedule(LocalDateTime.now().plusMinutes(10), "[15]");
        when(reminderLogMapper.selectCount(any())).thenReturn(1L);

        job.dispatch(s, LocalDateTime.now());

        verify(eventPublisher, never()).publish(any(), any());
        verify(reminderLogMapper, never()).insert(any(PmScheduleReminderLog.class));
    }

    @Test
    void notYetDueIsSkipped() {
        // start 在 2 小时后，提前 15 分钟提醒 → 提醒点未到
        PmSchedule s = schedule(LocalDateTime.now().plusHours(2), "[15]");

        job.dispatch(s, LocalDateTime.now());

        verify(eventPublisher, never()).publish(any(), any());
    }
}
