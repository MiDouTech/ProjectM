package com.mido.pm.task.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.outbox.DomainEventPublisher;
import com.mido.pm.task.dto.PersonWorkHourSummaryVO;
import com.mido.pm.task.dto.WorkHourCreateDTO;
import com.mido.pm.task.dto.WorkHourSummaryVO;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.task.entity.PmWorkHour;
import com.mido.pm.task.mapper.PmTaskMapper;
import com.mido.pm.task.mapper.PmWorkHourMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工时服务汇总口径单测（mock mapper，无 DB）：任务级聚合 est/actual 并套用口径（含预估为 0）；
 * 人员级按人分组；登记非法 kind/category 拒绝。
 */
@ExtendWith(MockitoExtension.class)
class WorkHourServiceTest {

    @Mock private PmWorkHourMapper workHourMapper;
    @Mock private PmTaskMapper taskMapper;
    @Mock private DomainEventPublisher eventPublisher;
    @InjectMocks private WorkHourService service;

    private PmWorkHour wh(Long userId, String kind, String hours) {
        PmWorkHour w = new PmWorkHour();
        w.setTaskId(1L);
        w.setUserId(userId);
        w.setKind(kind);
        w.setCategory("研发");
        w.setHours(new BigDecimal(hours));
        return w;
    }

    @Test
    void taskSummaryAggregatesAndAppliesCaliber() {
        when(workHourMapper.selectList(any())).thenReturn(List.of(
                wh(1L, "est", "8"), wh(1L, "actual", "6")));

        WorkHourSummaryVO s = service.taskSummary(1L);

        assertEquals(0, new BigDecimal("8").compareTo(s.estHours()));
        assertEquals(0, new BigDecimal("6").compareTo(s.actualHours()));
        assertEquals(new BigDecimal("75.0"), s.progress());
        assertEquals(new BigDecimal("2.00"), s.remainingHours());
    }

    @Test
    void taskSummaryIncludesSubtaskHours() {
        // 钉死口径：任务汇总 = 本任务 + 全部后代子任务工时
        // 第一层 frontier=[1] 返回子任务 2；第二层 frontier=[2] 无子任务 → 停
        when(taskMapper.selectList(any())).thenReturn(List.of(taskOf(2L)), List.of());
        when(workHourMapper.selectList(any())).thenReturn(List.of(
                wh(1L, "est", "4"), wh(1L, "actual", "1"),   // 父任务
                wh(2L, "actual", "3")));                       // 子任务

        WorkHourSummaryVO s = service.taskSummary(1L);

        assertEquals(0, new BigDecimal("4").compareTo(s.estHours()));
        assertEquals(0, new BigDecimal("4").compareTo(s.actualHours())); // 1 + 3 含子任务
        assertEquals(new BigDecimal("100.0"), s.progress());
    }

    @Test
    void taskSummaryProgressZeroWhenNoEstimate() {
        when(workHourMapper.selectList(any())).thenReturn(List.of(wh(1L, "actual", "5")));

        WorkHourSummaryVO s = service.taskSummary(1L);

        assertEquals(new BigDecimal("0.0"), s.progress());
        assertEquals(new BigDecimal("-5.00"), s.remainingHours());
    }

    @Test
    void personSummaryGroupsByUser() {
        when(taskMapper.selectList(any())).thenReturn(List.of(taskOf(1L)));
        when(workHourMapper.selectList(any())).thenReturn(List.of(
                wh(1L, "est", "10"), wh(1L, "actual", "4"),
                wh(2L, "actual", "9")));

        List<PersonWorkHourSummaryVO> list = service.personSummary(100L);

        assertEquals(2, list.size());
        // 按实际降序：用户2(9) 在前
        assertEquals(2L, list.get(0).userId());
        assertEquals(0, new BigDecimal("9").compareTo(list.get(0).actualHours()));
        assertEquals(1L, list.get(1).userId());
        assertEquals(new BigDecimal("40.0"), list.get(1).progress());
    }

    @Test
    void logRejectsIllegalKind() {
        assertThrows(BizException.class, () -> service.log(
                new WorkHourCreateDTO(1L, "bad", "研发", LocalDate.now(), new BigDecimal("2"), null)));
        verify(workHourMapper, never()).insert(any(PmWorkHour.class));
    }

    @Test
    void logRejectsIllegalCategory() {
        assertThrows(BizException.class, () -> service.log(
                new WorkHourCreateDTO(1L, "actual", "玄学", LocalDate.now(), new BigDecimal("2"), null)));
        verify(workHourMapper, never()).insert(any(PmWorkHour.class));
    }

    @Test
    void logPersistsAndEmitsEvent() {
        when(taskMapper.selectById(1L)).thenReturn(taskOf(1L));
        service.log(new WorkHourCreateDTO(1L, "actual", "研发", LocalDate.now(), new BigDecimal("3"), "联调"));
        verify(workHourMapper).insert(any(PmWorkHour.class));
        verify(eventPublisher).publish(eq("workhour.logged"), any());
    }

    private PmTask taskOf(Long id) {
        PmTask t = new PmTask();
        t.setId(id);
        return t;
    }
}
