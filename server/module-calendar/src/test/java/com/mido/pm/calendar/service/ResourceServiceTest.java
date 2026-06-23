package com.mido.pm.calendar.service;

import com.mido.pm.calendar.entity.PmScheduleResource;
import com.mido.pm.calendar.mapper.PmCalendarResourceMapper;
import com.mido.pm.calendar.mapper.PmScheduleMapper;
import com.mido.pm.calendar.mapper.PmScheduleResourceMapper;
import com.mido.pm.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 资源冲突检测单测（mock mapper）：同资源时间重叠抛 CONFLICT 且不写占用；错峰则放行并写占用。
 */
@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock private PmCalendarResourceMapper resourceMapper;
    @Mock private PmScheduleResourceMapper scheduleResourceMapper;
    @Mock private PmScheduleMapper scheduleMapper;
    @InjectMocks private ResourceService service;

    private final LocalDateTime start = LocalDateTime.of(2026, 6, 23, 10, 0);
    private final LocalDateTime end = start.plusHours(1);

    private void occupantExists() {
        PmScheduleResource occ = new PmScheduleResource();
        occ.setScheduleId(99L);
        occ.setResourceId(7L);
        when(scheduleResourceMapper.selectList(any())).thenReturn(List.of(occ));
    }

    @Test
    void bookingConflictThrowsAndSkipsInsert() {
        occupantExists();
        when(scheduleMapper.selectCount(any())).thenReturn(1L); // 存在时间重叠
        when(resourceMapper.selectById(7L)).thenReturn(null);

        assertThrows(BizException.class,
                () -> service.bookOrThrow(1L, List.of(7L), start, end, null));
        verify(scheduleResourceMapper, never()).insert(any(PmScheduleResource.class));
    }

    @Test
    void bookingWithoutOverlapInsertsLink() {
        occupantExists();
        when(scheduleMapper.selectCount(any())).thenReturn(0L); // 无重叠

        service.bookOrThrow(1L, List.of(7L), start, end, null);

        verify(scheduleResourceMapper).insert(any(PmScheduleResource.class));
    }

    @Test
    void emptyResourceIdsIsNoop() {
        service.bookOrThrow(1L, List.of(), start, end, null);
        verify(scheduleResourceMapper, never()).insert(any(PmScheduleResource.class));
        verify(scheduleResourceMapper, never()).selectList(any());
    }
}
