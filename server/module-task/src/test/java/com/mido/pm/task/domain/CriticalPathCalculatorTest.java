package com.mido.pm.task.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 关键路径计算单测（小图）：菱形图取最长链为关键路径，含浮动的旁支不计入。
 */
class CriticalPathCalculatorTest {

    @Test
    void diamondGraphPicksLongestPath() {
        // A1(3) B2(2) C3(4) D4(1)；A->B->D=6, A->C->D=8（关键）
        Map<Long, Long> dur = Map.of(1L, 3L, 2L, 2L, 3L, 4L, 4L, 1L);
        List<long[]> edges = List.of(
                new long[]{1, 2}, new long[]{2, 4}, new long[]{1, 3}, new long[]{3, 4});

        CriticalPathCalculator.Result r = CriticalPathCalculator.compute(dur, edges);

        assertEquals(8L, r.totalDurationDays());
        assertEquals(java.util.Set.of(1L, 3L, 4L), r.criticalTaskIds()); // B2 有浮动，被排除
    }

    @Test
    void singleChainAllCritical() {
        // A1(2)->B2(3)->C3(1)，链上全部关键，工期 6
        Map<Long, Long> dur = Map.of(1L, 2L, 2L, 3L, 3L, 1L);
        List<long[]> edges = List.of(new long[]{1, 2}, new long[]{2, 3});

        CriticalPathCalculator.Result r = CriticalPathCalculator.compute(dur, edges);

        assertEquals(6L, r.totalDurationDays());
        assertEquals(java.util.Set.of(1L, 2L, 3L), r.criticalTaskIds());
    }

    @Test
    void isolatedShorterTaskHasSlackNotCritical() {
        // 链 A1(5)->B2(5)=10；孤立 C3(2)。关键集 {A,B}，C 有浮动
        Map<Long, Long> dur = Map.of(1L, 5L, 2L, 5L, 3L, 2L);
        List<long[]> edges = List.of(new long[]{1, 2});

        CriticalPathCalculator.Result r = CriticalPathCalculator.compute(dur, edges);

        assertEquals(10L, r.totalDurationDays());
        assertEquals(java.util.Set.of(1L, 2L), r.criticalTaskIds());
    }

    @Test
    void emptyGraphNoCritical() {
        CriticalPathCalculator.Result r = CriticalPathCalculator.compute(Map.of(), List.of());
        assertEquals(0L, r.totalDurationDays());
        assertTrue(r.criticalTaskIds().isEmpty());
    }
}
