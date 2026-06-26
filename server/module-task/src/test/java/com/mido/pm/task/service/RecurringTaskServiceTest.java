package com.mido.pm.task.service;

import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.mapper.PmTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 循环任务实例生成单测：封顶、count/until 约束、幂等去重、无锚定不生成。 */
@ExtendWith(MockitoExtension.class)
class RecurringTaskServiceTest {

    @Mock
    private PmTaskMapper taskMapper;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private WorkItemMetaResolver metaResolver;
    @InjectMocks
    private RecurringTaskService service;

    private PmTask template(String recurRule, LocalDate start, LocalDate due) {
        PmTask t = new PmTask();
        t.setId(100L);
        t.setProjectId(1L);
        t.setTitle("周会纪要");
        t.setStartDate(start);
        t.setDueDate(due);
        t.setRecurRule(recurRule);
        return t;
    }

    @Test
    void missingTaskThrows() {
        when(taskMapper.selectById(100L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.generate(100L));
    }

    @Test
    void noRuleGeneratesNothing() {
        when(taskMapper.selectById(100L)).thenReturn(template(null, LocalDate.of(2026, 1, 1), null));
        assertEquals(0, service.generate(100L));
        verify(taskMapper, never()).insert(any(PmTask.class));
    }

    @Test
    void noAnchorDateGeneratesNothing() {
        when(taskMapper.selectById(100L)).thenReturn(template("{\"freq\":\"WEEKLY\"}", null, null));
        assertEquals(0, service.generate(100L));
        verify(taskMapper, never()).insert(any(PmTask.class));
    }

    @Test
    void weeklyNoCountGeneratesUpToCap() {
        when(taskMapper.selectById(100L)).thenReturn(template("{\"freq\":\"WEEKLY\"}", LocalDate.of(2026, 1, 1), null));
        when(taskMapper.selectList(any())).thenReturn(List.of());
        assertEquals(RecurringTaskService.MAX_GENERATE, service.generate(100L));
        verify(taskMapper, times(RecurringTaskService.MAX_GENERATE)).insert(any(PmTask.class));
    }

    @Test
    void countCapsInstancesToCountMinusOne() {
        when(taskMapper.selectById(100L)).thenReturn(
                template("{\"freq\":\"WEEKLY\",\"count\":4}", LocalDate.of(2026, 1, 1), null));
        when(taskMapper.selectList(any())).thenReturn(List.of());
        assertEquals(3, service.generate(100L));
    }

    @Test
    void untilStopsGeneration() {
        when(taskMapper.selectById(100L)).thenReturn(
                template("{\"freq\":\"WEEKLY\",\"until\":\"2026-01-22\"}", LocalDate.of(2026, 1, 1), null));
        when(taskMapper.selectList(any())).thenReturn(List.of());
        // 01-08 / 01-15 / 01-22 命中，01-29 超过 until
        assertEquals(3, service.generate(100L));
    }

    @Test
    void skipsExistingOccurrenceDates() {
        when(taskMapper.selectById(100L)).thenReturn(template("{\"freq\":\"WEEKLY\"}", LocalDate.of(2026, 1, 1), null));
        PmTask existing = new PmTask();
        existing.setStartDate(LocalDate.of(2026, 1, 8)); // 第 1 个实例已存在
        lenient().when(taskMapper.selectList(any())).thenReturn(List.of(existing));
        assertEquals(RecurringTaskService.MAX_GENERATE - 1, service.generate(100L));
    }
}
