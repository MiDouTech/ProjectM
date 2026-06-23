package com.mido.pm.calendar.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 排期小助手核心（纯逻辑）：在工作时段 [from,to] 内，扣除所有忙闲区间后，
 * 返回时长不小于 durationMinutes 的连续空档。
 */
public final class SlotFinder {

    private SlotFinder() {
    }

    public record Interval(LocalDateTime start, LocalDateTime end) {
    }

    public static List<Interval> freeSlots(List<Interval> busy, LocalDateTime from, LocalDateTime to,
                                           int durationMinutes) {
        List<Interval> merged = mergeClipped(busy, from, to);
        List<Interval> slots = new ArrayList<>();
        LocalDateTime cursor = from;
        for (Interval b : merged) {
            if (b.start().isAfter(cursor)) {
                addIfFits(slots, cursor, b.start(), durationMinutes);
            }
            if (b.end().isAfter(cursor)) {
                cursor = b.end();
            }
        }
        if (cursor.isBefore(to)) {
            addIfFits(slots, cursor, to, durationMinutes);
        }
        return slots;
    }

    private static void addIfFits(List<Interval> slots, LocalDateTime s, LocalDateTime e, int durationMinutes) {
        if (Duration.between(s, e).toMinutes() >= durationMinutes) {
            slots.add(new Interval(s, e));
        }
    }

    /** 将忙闲区间裁剪到窗口内、排序并合并重叠/相邻段。 */
    private static List<Interval> mergeClipped(List<Interval> busy, LocalDateTime from, LocalDateTime to) {
        List<Interval> clipped = new ArrayList<>();
        if (busy != null) {
            for (Interval b : busy) {
                LocalDateTime s = b.start().isBefore(from) ? from : b.start();
                LocalDateTime e = b.end().isAfter(to) ? to : b.end();
                if (s.isBefore(e)) {
                    clipped.add(new Interval(s, e));
                }
            }
        }
        clipped.sort(Comparator.comparing(Interval::start));
        List<Interval> merged = new ArrayList<>();
        for (Interval b : clipped) {
            if (merged.isEmpty() || b.start().isAfter(merged.get(merged.size() - 1).end())) {
                merged.add(b);
            } else {
                Interval last = merged.remove(merged.size() - 1);
                LocalDateTime end = last.end().isAfter(b.end()) ? last.end() : b.end();
                merged.add(new Interval(last.start(), end));
            }
        }
        return merged;
    }
}
