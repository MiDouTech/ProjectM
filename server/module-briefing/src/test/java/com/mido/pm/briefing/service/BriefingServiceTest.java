package com.mido.pm.briefing.service;

import com.mido.pm.briefing.dto.BriefingSaveDTO;
import com.mido.pm.briefing.entity.PmBriefing;
import com.mido.pm.briefing.entity.PmBriefingTemplate;
import com.mido.pm.briefing.event.BriefingEvents;
import com.mido.pm.briefing.mapper.PmBriefingMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 简报服务单测：幂等 upsert、已提交不可改、提交发事件与权限。 */
@ExtendWith(MockitoExtension.class)
class BriefingServiceTest {

    private static final long ME = 100L;

    @Mock private PmBriefingMapper briefingMapper;
    @Mock private BriefingTemplateService templateService;
    @Mock private BriefingReviewService reviewService;
    @Mock private DomainEventPublisher eventPublisher;
    @InjectMocks private BriefingService service;

    @BeforeEach
    void setUp() {
        CurrentUser u = new CurrentUser();
        u.setUserId(ME);
        u.setDeptId(7L);
        UserContext.set(u);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private BriefingSaveDTO dto() {
        return new BriefingSaveDTO(1L, "2026-06-23", LocalDate.of(2026, 6, 23),
                LocalDate.of(2026, 6, 23), Map.of("todayDone", "完成了 A"));
    }

    private PmBriefingTemplate template() {
        PmBriefingTemplate t = new PmBriefingTemplate();
        t.setId(1L);
        t.setType("daily");
        return t;
    }

    @Test
    void saveInsertsWhenAbsent() {
        when(templateService.require(1L)).thenReturn(template());
        when(briefingMapper.selectOne(any())).thenReturn(null);
        service.save(dto());
        verify(briefingMapper).insert(any(PmBriefing.class));
    }

    @Test
    void saveUpdatesExistingDraft() {
        when(templateService.require(1L)).thenReturn(template());
        PmBriefing existing = new PmBriefing();
        existing.setId(9L);
        existing.setStatus("draft");
        when(briefingMapper.selectOne(any())).thenReturn(existing);
        Long id = service.save(dto());
        assertEquals(9L, id);
        verify(briefingMapper).updateById(existing);
        verify(briefingMapper, never()).insert(any(PmBriefing.class));
    }

    @Test
    void saveRejectsSubmitted() {
        when(templateService.require(1L)).thenReturn(template());
        PmBriefing existing = new PmBriefing();
        existing.setStatus("submitted");
        when(briefingMapper.selectOne(any())).thenReturn(existing);
        assertThrows(BizException.class, () -> service.save(dto()));
    }

    @Test
    void submitSetsStatusAndPublishes() {
        PmBriefing b = new PmBriefing();
        b.setId(9L);
        b.setAuthorId(ME);
        b.setType("daily");
        b.setStatus("draft");
        when(briefingMapper.selectById(9L)).thenReturn(b);

        service.submit(9L);

        assertEquals("submitted", b.getStatus());
        verify(briefingMapper).updateById(b);
        verify(eventPublisher).publish(eq(BriefingEvents.SUBMITTED), any());
    }

    @Test
    void submitRejectsNonOwner() {
        PmBriefing b = new PmBriefing();
        b.setAuthorId(999L);
        b.setStatus("draft");
        when(briefingMapper.selectById(9L)).thenReturn(b);
        assertThrows(BizException.class, () -> service.submit(9L));
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    void submitRejectsAlreadySubmitted() {
        PmBriefing b = new PmBriefing();
        b.setAuthorId(ME);
        b.setStatus("submitted");
        when(briefingMapper.selectById(9L)).thenReturn(b);
        assertThrows(BizException.class, () -> service.submit(9L));
    }
}
