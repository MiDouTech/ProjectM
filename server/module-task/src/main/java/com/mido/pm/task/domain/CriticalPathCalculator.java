package com.mido.pm.task.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 关键路径计算（纯函数，无 DB）。基于 FS 依赖（前置完成后后继开始）与任务工期，做 CPM 正/反向遍历：
 * 正向求最早开始/完成(ES/EF)，反向求最晚开始/完成(LS/LF)，总浮动 slack=LS-ES=0 者为关键任务。
 * 入参图须为 DAG（新增依赖时已做循环检测保证）。
 */
public final class CriticalPathCalculator {

    /** @param criticalTaskIds 关键任务集；@param totalDurationDays 项目工期（最长路径长度，天） */
    public record Result(Set<Long> criticalTaskIds, long totalDurationDays) {
    }

    private CriticalPathCalculator() {
    }

    /**
     * @param durations 任务 id → 工期（天，&gt;=0）
     * @param edges     依赖边 [predecessorId, successorId]
     */
    public static Result compute(Map<Long, Long> durations, List<long[]> edges) {
        if (durations.isEmpty()) {
            return new Result(Set.of(), 0L);
        }
        Map<Long, List<Long>> succ = new HashMap<>();
        Map<Long, Integer> indegree = new HashMap<>();
        durations.keySet().forEach(id -> {
            succ.put(id, new ArrayList<>());
            indegree.put(id, 0);
        });
        for (long[] e : edges) {
            // 仅纳入 durations 内的节点，忽略悬挂边
            if (!durations.containsKey(e[0]) || !durations.containsKey(e[1])) {
                continue;
            }
            succ.get(e[0]).add(e[1]);
            indegree.merge(e[1], 1, Integer::sum);
        }

        List<Long> order = topo(durations.keySet(), succ, indegree);

        // 正向：ES/EF
        Map<Long, Long> es = new HashMap<>();
        Map<Long, Long> ef = new HashMap<>();
        durations.keySet().forEach(id -> es.put(id, 0L));
        for (Long t : order) {
            long finish = es.get(t) + durations.get(t);
            ef.put(t, finish);
            for (Long s : succ.get(t)) {
                es.merge(s, finish, Math::max);
            }
        }
        long projectFinish = ef.values().stream().mapToLong(Long::longValue).max().orElse(0L);

        // 反向：LF/LS（逆拓扑序，后继已定）
        Map<Long, Long> lf = new HashMap<>();
        Map<Long, Long> ls = new HashMap<>();
        for (int i = order.size() - 1; i >= 0; i--) {
            Long t = order.get(i);
            long latestFinish = succ.get(t).isEmpty() ? projectFinish
                    : succ.get(t).stream().mapToLong(ls::get).min().orElse(projectFinish);
            lf.put(t, latestFinish);
            ls.put(t, latestFinish - durations.get(t));
        }

        // slack=0 为关键任务（保序）
        Set<Long> critical = new LinkedHashSet<>();
        for (Long t : order) {
            if (ls.get(t).equals(es.get(t))) {
                critical.add(t);
            }
        }
        return new Result(critical, projectFinish);
    }

    /** Kahn 拓扑排序；图为 DAG，残余节点（异常成环）按入度 0 不可达则忽略。 */
    private static List<Long> topo(Set<Long> nodes, Map<Long, List<Long>> succ, Map<Long, Integer> indegree) {
        Map<Long, Integer> deg = new HashMap<>(indegree);
        Deque<Long> queue = new ArrayDeque<>();
        nodes.forEach(n -> {
            if (deg.get(n) == 0) {
                queue.add(n);
            }
        });
        List<Long> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long n = queue.poll();
            order.add(n);
            for (Long s : succ.get(n)) {
                if (deg.merge(s, -1, Integer::sum) == 0) {
                    queue.add(s);
                }
            }
        }
        return order;
    }
}
