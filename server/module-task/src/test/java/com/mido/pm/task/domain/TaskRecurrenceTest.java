package com.mido.pm.task.domain;

import com.mido.pm.common.exception.BizException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 循环规则解析与步进单测。 */
class TaskRecurrenceTest {

    @Test
    void blankRuleReturnsNull() {
        assertNull(TaskRecurrence.parse(null));
        assertNull(TaskRecurrence.parse(""));
        assertNull(TaskRecurrence.parse("  "));
    }

    @Test
    void parsesFullRule() {
        TaskRecurrence r = TaskRecurrence.parse("{\"freq\":\"WEEKLY\",\"interval\":2,\"count\":10,\"until\":\"2026-12-31\"}");
        assertEquals("WEEKLY", r.freq());
        assertEquals(2, r.interval());
        assertEquals(10, r.count());
        assertEquals(LocalDate.of(2026, 12, 31), r.until());
    }

    @Test
    void defaultsIntervalAndNullableCountUntil() {
        TaskRecurrence r = TaskRecurrence.parse("{\"freq\":\"DAILY\"}");
        assertEquals(1, r.interval());
        assertNull(r.count());
        assertNull(r.until());
    }

    @Test
    void rejectsIllegalFreq() {
        assertThrows(BizException.class, () -> TaskRecurrence.parse("{\"freq\":\"HOURLY\"}"));
        assertThrows(BizException.class, () -> TaskRecurrence.parse("{\"interval\":1}"));
        assertThrows(BizException.class, () -> TaskRecurrence.parse("not-json"));
    }

    @Test
    void shiftSteps() {
        LocalDate base = LocalDate.of(2026, 1, 1);
        assertEquals(base.plusDays(3), TaskRecurrence.parse("{\"freq\":\"DAILY\"}").shift(base, 3));
        assertEquals(base.plusWeeks(4), TaskRecurrence.parse("{\"freq\":\"WEEKLY\",\"interval\":2}").shift(base, 2));
        assertEquals(base.plusMonths(2), TaskRecurrence.parse("{\"freq\":\"MONTHLY\"}").shift(base, 2));
        assertEquals(base.plusYears(1), TaskRecurrence.parse("{\"freq\":\"YEARLY\"}").shift(base, 1));
    }
}
