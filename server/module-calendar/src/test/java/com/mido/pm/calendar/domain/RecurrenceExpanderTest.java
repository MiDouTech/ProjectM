package com.mido.pm.calendar.domain;

import com.mido.pm.calendar.entity.PmSchedule;
import com.mido.pm.calendar.entity.PmScheduleException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 循环展开单测：count/until 终止、cancel 剔除、modify 覆盖。 */
class RecurrenceExpanderTest {

    private PmSchedule weekly(String rule) {
        PmSchedule s = new PmSchedule();
        s.setId(1L);
        s.setTitle("周会");
        s.setStartTime(LocalDateTime.of(2026, 6, 1, 10, 0)); // 周一
        s.setEndTime(LocalDateTime.of(2026, 6, 1, 11, 0));
        s.setRecurRule(rule);
        return s;
    }

    private final LocalDateTime from = LocalDateTime.of(2026, 6, 1, 0, 0);
    private final LocalDateTime to = LocalDateTime.of(2026, 6, 30, 23, 59);

    @Test
    void weeklyCountExpandsExpectedOccurrences() {
        var occ = RecurrenceExpander.expand(weekly("{\"freq\":\"WEEKLY\",\"interval\":1,\"count\":4}"),
                List.of(), from, to);
        assertEquals(4, occ.size());
        assertEquals(LocalDate.of(2026, 6, 1), occ.get(0).occurrenceDate());
        assertEquals(LocalDate.of(2026, 6, 22), occ.get(3).occurrenceDate());
    }

    @Test
    void untilStopsSeries() {
        var occ = RecurrenceExpander.expand(weekly("{\"freq\":\"WEEKLY\",\"interval\":1,\"until\":\"2026-06-15\"}"),
                List.of(), from, to);
        assertEquals(3, occ.size()); // 6-1, 6-8, 6-15
    }

    @Test
    void cancelExceptionRemovesOccurrence() {
        PmScheduleException ex = new PmScheduleException();
        ex.setScheduleId(1L);
        ex.setOccurDate(LocalDate.of(2026, 6, 8));
        ex.setAction("cancel");
        var occ = RecurrenceExpander.expand(weekly("{\"freq\":\"WEEKLY\",\"interval\":1,\"count\":4}"),
                List.of(ex), from, to);
        assertEquals(3, occ.size());
        assertTrue(occ.stream().noneMatch(o -> o.occurrenceDate().equals(LocalDate.of(2026, 6, 8))));
    }

    @Test
    void modifyExceptionOverridesTitle() {
        PmScheduleException ex = new PmScheduleException();
        ex.setScheduleId(1L);
        ex.setOccurDate(LocalDate.of(2026, 6, 15));
        ex.setAction("modify");
        ex.setOverride("{\"title\":\"临时改名\"}");
        var occ = RecurrenceExpander.expand(weekly("{\"freq\":\"WEEKLY\",\"interval\":1,\"count\":4}"),
                List.of(ex), from, to);
        var modified = occ.stream().filter(o -> o.occurrenceDate().equals(LocalDate.of(2026, 6, 15)))
                .findFirst().orElseThrow();
        assertEquals("临时改名", modified.title());
    }

    @Test
    void nonRecurringReturnsEmpty() {
        PmSchedule s = weekly(null);
        assertTrue(RecurrenceExpander.expand(s, List.of(), from, to).isEmpty());
    }

    @Test
    void longRunningDailyFastForwardsIntoWindow() {
        // 起始于 ~5 年前的每日循环、无 count/until；查询近期 7 天窗口应得 7 次（而非因 1000 步上限漏空）。
        PmSchedule s = new PmSchedule();
        s.setId(1L);
        s.setTitle("站会");
        s.setStartTime(LocalDateTime.of(2021, 1, 1, 9, 0));
        s.setEndTime(LocalDateTime.of(2021, 1, 1, 9, 30));
        s.setRecurRule("{\"freq\":\"DAILY\",\"interval\":1}");
        LocalDateTime w0 = LocalDateTime.of(2026, 6, 23, 0, 0);
        LocalDateTime w1 = LocalDateTime.of(2026, 6, 29, 23, 59);
        var occ = RecurrenceExpander.expand(s, List.of(), w0, w1);
        assertEquals(7, occ.size());
        assertEquals(LocalDate.of(2026, 6, 23), occ.get(0).occurrenceDate());
    }

    @Test
    void malformedRuleReturnsEmptyNotThrow() {
        PmSchedule s = weekly("not-json");
        assertTrue(RecurrenceExpander.expand(s, List.of(), from, to).isEmpty());
        PmSchedule s2 = weekly("{\"freq\":\"WEEKLY\",\"until\":\"2026-13-99\"}");
        assertTrue(RecurrenceExpander.expand(s2, List.of(), from, to).isEmpty());
    }
}
