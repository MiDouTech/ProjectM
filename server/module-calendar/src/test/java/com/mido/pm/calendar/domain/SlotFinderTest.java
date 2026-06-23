package com.mido.pm.calendar.domain;

import com.mido.pm.calendar.domain.SlotFinder.Interval;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 排期小助手单测：空档计算、时长过滤、全忙、无忙、相邻合并。 */
class SlotFinderTest {

    private LocalDateTime t(int h, int m) {
        return LocalDateTime.of(2026, 6, 23, h, m);
    }

    @Test
    void gapsAroundBusyBlocks() {
        List<Interval> busy = List.of(new Interval(t(10, 0), t(11, 0)), new Interval(t(14, 0), t(15, 0)));
        List<Interval> slots = SlotFinder.freeSlots(busy, t(9, 0), t(18, 0), 60);
        assertEquals(3, slots.size());
        assertEquals(t(9, 0), slots.get(0).start());
        assertEquals(t(10, 0), slots.get(0).end());
        assertEquals(t(11, 0), slots.get(1).start());
        assertEquals(t(15, 0), slots.get(2).start());
    }

    @Test
    void durationFiltersTooShortGaps() {
        List<Interval> busy = List.of(new Interval(t(10, 0), t(11, 0)), new Interval(t(14, 0), t(15, 0)));
        // 9-10(60min) 被 120min 时长过滤掉；剩 11-14、15-18
        List<Interval> slots = SlotFinder.freeSlots(busy, t(9, 0), t(18, 0), 120);
        assertEquals(2, slots.size());
        assertEquals(t(11, 0), slots.get(0).start());
        assertEquals(t(15, 0), slots.get(1).start());
    }

    @Test
    void noBusyReturnsWholeWindow() {
        List<Interval> slots = SlotFinder.freeSlots(List.of(), t(9, 0), t(18, 0), 60);
        assertEquals(1, slots.size());
        assertEquals(t(9, 0), slots.get(0).start());
        assertEquals(t(18, 0), slots.get(0).end());
    }

    @Test
    void fullyBusyReturnsEmpty() {
        List<Interval> slots = SlotFinder.freeSlots(List.of(new Interval(t(8, 0), t(19, 0))),
                t(9, 0), t(18, 0), 30);
        assertEquals(0, slots.size());
    }

    @Test
    void overlappingBusyMerged() {
        List<Interval> busy = List.of(new Interval(t(10, 0), t(12, 0)), new Interval(t(11, 0), t(13, 0)));
        List<Interval> slots = SlotFinder.freeSlots(busy, t(9, 0), t(18, 0), 60);
        // 合并为 10-13，剩 9-10、13-18
        assertEquals(2, slots.size());
        assertEquals(t(13, 0), slots.get(1).start());
    }
}
