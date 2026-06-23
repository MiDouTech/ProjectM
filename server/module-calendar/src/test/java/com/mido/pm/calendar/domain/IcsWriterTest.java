package com.mido.pm.calendar.domain;

import com.mido.pm.calendar.domain.IcsWriter.VEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ics 生成单测：结构正确、含事件字段、特殊字符转义。 */
class IcsWriterTest {

    @Test
    void writesVCalendarWithEvent() {
        String ics = IcsWriter.write("我的日程", List.of(
                new VEvent("u1@mido", "周会", LocalDateTime.of(2026, 6, 23, 10, 0),
                        LocalDateTime.of(2026, 6, 23, 11, 0), "会议室A", false)));
        assertTrue(ics.startsWith("BEGIN:VCALENDAR"));
        assertTrue(ics.contains("BEGIN:VEVENT"));
        assertTrue(ics.contains("UID:u1@mido"));
        assertTrue(ics.contains("DTSTART:20260623T100000"));
        assertTrue(ics.contains("DTEND:20260623T110000"));
        assertTrue(ics.contains("SUMMARY:周会"));
        assertTrue(ics.contains("LOCATION:会议室A"));
        assertTrue(ics.trim().endsWith("END:VCALENDAR"));
    }

    @Test
    void allDayUsesDateValue() {
        String ics = IcsWriter.write("日历", List.of(
                new VEvent("u2", "假期", LocalDateTime.of(2026, 6, 23, 0, 0),
                        LocalDateTime.of(2026, 6, 24, 0, 0), null, true)));
        assertTrue(ics.contains("DTSTART;VALUE=DATE:20260623"));
        // RFC 5545：全天 DTEND 排他（末日 06-24 → DTEND 06-25），不得等于 DTSTART
        assertTrue(ics.contains("DTEND;VALUE=DATE:20260625"));
        assertFalse(ics.contains("LOCATION:"));
    }

    @Test
    void escapesSpecialChars() {
        String ics = IcsWriter.write("c", List.of(
                new VEvent("u3", "A;B,C", LocalDateTime.of(2026, 6, 23, 9, 0),
                        LocalDateTime.of(2026, 6, 23, 10, 0), null, false)));
        assertTrue(ics.contains("SUMMARY:A\\;B\\,C"));
    }
}
