package com.mido.pm.task.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 任务依赖图算法（纯函数，无 DB）：新增依赖边时的循环依赖检测。
 * 邻接表为 predecessor → [successors]。新增边 from(前置)→to(后继) 若使图成环则返回环路径，否则返回空列表。
 */
public final class TaskDependencyGraph {

    private TaskDependencyGraph() {
    }

    /**
     * 检测新增边 from→to 是否成环。
     * 成环条件：to 经现有边可达 from（则 from→to 闭合 from→to→…→from）。
     *
     * @return 环路径 [from, to, …, from]；无环返回空列表
     */
    public static List<Long> detectCycle(Map<Long, List<Long>> adjacency, Long from, Long to) {
        if (from.equals(to)) {
            return List.of(from, to); // 自环
        }
        List<Long> path = new ArrayList<>();
        if (dfs(adjacency, to, from, new HashSet<>(), path)) {
            List<Long> cycle = new ArrayList<>();
            cycle.add(from);
            cycle.addAll(path); // path: [to, …, from]
            return cycle;
        }
        return List.of();
    }

    /** 从 cur 沿邻接边搜索 target，命中则 path 记录 [cur, …, target]。 */
    private static boolean dfs(Map<Long, List<Long>> adjacency, Long cur, Long target,
                               Set<Long> visited, List<Long> path) {
        if (!visited.add(cur)) {
            return false;
        }
        path.add(cur);
        if (cur.equals(target)) {
            return true;
        }
        for (Long next : adjacency.getOrDefault(cur, List.of())) {
            if (dfs(adjacency, next, target, visited, path)) {
                return true;
            }
        }
        path.remove(path.size() - 1);
        return false;
    }
}
