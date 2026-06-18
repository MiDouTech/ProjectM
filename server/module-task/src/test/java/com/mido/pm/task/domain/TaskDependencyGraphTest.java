package com.mido.pm.task.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 循环依赖检测单测（小图）：新增边成环则返回环路径，否则空。
 */
class TaskDependencyGraphTest {

    // 现有边 A(1)->B(2), B(2)->C(3)
    private Map<Long, List<Long>> chain() {
        return Map.of(1L, List.of(2L), 2L, List.of(3L));
    }

    @Test
    void noCycleForAcyclicEdge() {
        // 新增 B(2)->D(4)：不成环
        assertTrue(TaskDependencyGraph.detectCycle(chain(), 2L, 4L).isEmpty());
    }

    @Test
    void detectsBackEdgeCycle() {
        // 新增 C(3)->A(1)：闭合 C->A->B->C
        List<Long> cycle = TaskDependencyGraph.detectCycle(chain(), 3L, 1L);
        assertEquals(List.of(3L, 1L, 2L, 3L), cycle);
    }

    @Test
    void detectsSelfLoop() {
        assertEquals(List.of(1L, 1L), TaskDependencyGraph.detectCycle(chain(), 1L, 1L));
    }

    @Test
    void noCycleWhenTargetUnreachable() {
        // 新增 A(1)->C(3)：A 无法经现有边回到自身，不成环
        assertTrue(TaskDependencyGraph.detectCycle(chain(), 1L, 3L).isEmpty());
    }
}
