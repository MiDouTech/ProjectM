package com.mido.pm.calendar.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * iCalendar(RFC 5545) 最小集生成器（纯逻辑，不引第三方 ical 库）。
 * 采用浮动本地时间（不带 TZID），满足订阅展示需求。
 */
public final class IcsWriter {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String CRLF = "\r\n";

    private IcsWriter() {
    }

    public record VEvent(String uid, String title, LocalDateTime start, LocalDateTime end,
                         String location, boolean allDay) {
    }

    public static String write(String calendarName, List<VEvent> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR").append(CRLF);
        sb.append("VERSION:2.0").append(CRLF);
        sb.append("PRODID:-//mido-pm//calendar//CN").append(CRLF);
        sb.append("CALSCALE:GREGORIAN").append(CRLF);
        sb.append("X-WR-CALNAME:").append(escape(calendarName)).append(CRLF);
        String stamp = LocalDateTime.now().format(DT);
        for (VEvent e : events) {
            sb.append("BEGIN:VEVENT").append(CRLF);
            sb.append("UID:").append(e.uid()).append(CRLF);
            sb.append("DTSTAMP:").append(stamp).append(CRLF);
            if (e.allDay()) {
                sb.append("DTSTART;VALUE=DATE:").append(e.start().format(DATE)).append(CRLF);
                sb.append("DTEND;VALUE=DATE:").append(e.end().format(DATE)).append(CRLF);
            } else {
                sb.append("DTSTART:").append(e.start().format(DT)).append(CRLF);
                sb.append("DTEND:").append(e.end().format(DT)).append(CRLF);
            }
            sb.append("SUMMARY:").append(escape(e.title())).append(CRLF);
            if (e.location() != null && !e.location().isBlank()) {
                sb.append("LOCATION:").append(escape(e.location())).append(CRLF);
            }
            sb.append("END:VEVENT").append(CRLF);
        }
        sb.append("END:VCALENDAR").append(CRLF);
        return sb.toString();
    }

    private static String escape(String v) {
        if (v == null) {
            return "";
        }
        return v.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }
}
