package com.mido.pm.briefing.job;

import com.mido.pm.briefing.entity.PmBriefing;
import com.mido.pm.briefing.event.BriefingEvents;
import com.mido.pm.briefing.mapper.PmBriefingMapper;
import com.mido.pm.common.outbox.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 简报催交单测：每条到点草稿发一条 briefing.reminder.due。 */
@ExtendWith(MockitoExtension.class)
class BriefingReminderJobTest {

    @Mock private PmBriefingMapper briefingMapper;
    @Mock private DomainEventPublisher eventPublisher;
    @InjectMocks private BriefingReminderJob job;

    private PmBriefing draft(Long id, Long author) {
        PmBriefing b = new PmBriefing();
        b.setId(id);
        b.setAuthorId(author);
        b.setType("daily");
        b.setPeriodKey("2026-06-23");
        return b;
    }

    @Test
    void publishesReminderPerDueDraft() {
        when(briefingMapper.selectList(any())).thenReturn(List.of(draft(1L, 10L), draft(2L, 20L)));
        job.scan();
        verify(eventPublisher, times(2)).publish(eq(BriefingEvents.REMINDER_DUE), any());
    }

    @Test
    void noDraftsNoEvents() {
        when(briefingMapper.selectList(any())).thenReturn(List.of());
        job.scan();
        verify(eventPublisher, times(0)).publish(any(), any());
    }
}
