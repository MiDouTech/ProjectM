package com.mido.pm.task.domain;

import com.mido.pm.task.dto.TaskVO;
import com.mido.pm.view.dto.ViewConfig;
import com.mido.pm.view.dto.ViewConfig.FilterCondition;
import com.mido.pm.view.dto.ViewConfig.FilterGroup;
import com.mido.pm.view.dto.ViewConfig.SortSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 视图自定义字段 split/matches/comparator 纯函数单测。 */
class TaskViewCustomFieldTest {

    private TaskVO task(Long id, String status, Map<String, String> cf) {
        return new TaskVO(id, 1L, null, "t" + id, null, null, status, 1, null,
                null, null, 0, null).withCustomFields(cf);
    }

    private final Map<String, String> cfTypes = Map.of("level", "number", "owner", "text", "due", "date");

    @Test
    void splitSeparatesNativeAndCf() {
        ViewConfig config = new ViewConfig(null, null, 1,
                new FilterGroup("and", List.of(
                        new FilterCondition("status", "eq", "进行中"),
                        new FilterCondition("cf:level", "ge", "3"))),
                List.of("title", "cf:owner"));
        TaskViewCustomField.Split split = TaskViewCustomField.split(config);
        // 原生条件下推（nativeConfig.filters 仅 status），cf 留内存
        assertEquals(1, split.nativeConfig().filters().conditions().size());
        assertEquals("status", split.nativeConfig().filters().conditions().get(0).field());
        assertEquals(1, split.memoryFilter().size());
        assertEquals("cf:level", split.memoryFilter().get(0).field());
    }

    @Test
    void splitOrWithCfKeepsAllInMemory() {
        ViewConfig config = new ViewConfig(null, null, 1,
                new FilterGroup("or", List.of(
                        new FilterCondition("status", "eq", "进行中"),
                        new FilterCondition("cf:level", "ge", "3"))),
                List.of());
        TaskViewCustomField.Split split = TaskViewCustomField.split(config);
        assertEquals(null, split.nativeConfig().filters()); // 不下推任何筛选
        assertEquals(2, split.memoryFilter().size());
        assertEquals("or", split.memoryLogic());
    }

    @Test
    void splitCfSortGoesAllToMemory() {
        ViewConfig config = new ViewConfig(null,
                List.of(new SortSpec("cf:level", "desc"), new SortSpec("status", "asc")),
                1, null, List.of());
        TaskViewCustomField.Split split = TaskViewCustomField.split(config);
        assertTrue(split.nativeConfig().sort().isEmpty());
        assertEquals(2, split.memorySort().size());
    }

    @Test
    void matchesNumberGe() {
        TaskVO t = task(1L, "doing", Map.of("level", "5"));
        List<FilterCondition> conds = List.of(new FilterCondition("cf:level", "ge", "3"));
        assertTrue(TaskViewCustomField.matches(t, conds, "and", cfTypes));
        List<FilterCondition> conds2 = List.of(new FilterCondition("cf:level", "ge", "9"));
        assertFalse(TaskViewCustomField.matches(t, conds2, "and", cfTypes));
    }

    @Test
    void matchesNumberDecimalEquality() {
        TaskVO t = task(1L, "doing", Map.of("level", "12.5"));
        // 入库 12.5 与条件 12.50 数值等价
        assertTrue(TaskViewCustomField.matches(t,
                List.of(new FilterCondition("cf:level", "eq", "12.50")), "and", cfTypes));
    }

    @Test
    void matchesTextLikeAndNull() {
        TaskVO t = task(1L, "doing", Map.of("owner", "Alice"));
        assertTrue(TaskViewCustomField.matches(t,
                List.of(new FilterCondition("cf:owner", "like", "ali")), "and", cfTypes));
        // 无 due 值 → isNull 命中
        assertTrue(TaskViewCustomField.matches(t,
                List.of(new FilterCondition("cf:due", "isNull", null)), "and", cfTypes));
        assertFalse(TaskViewCustomField.matches(t,
                List.of(new FilterCondition("cf:due", "notNull", null)), "and", cfTypes));
    }

    @Test
    void matchesOrLogic() {
        TaskVO t = task(1L, "done", Map.of("level", "1"));
        List<FilterCondition> conds = List.of(
                new FilterCondition("status", "eq", "doing"),
                new FilterCondition("cf:level", "eq", "1"));
        assertTrue(TaskViewCustomField.matches(t, conds, "or", cfTypes));
        List<FilterCondition> none = List.of(
                new FilterCondition("status", "eq", "doing"),
                new FilterCondition("cf:level", "eq", "9"));
        assertFalse(TaskViewCustomField.matches(t, none, "or", cfTypes));
    }

    @Test
    void matchesInOperator() {
        TaskVO t = task(1L, "doing", Map.of("owner", "bob"));
        assertTrue(TaskViewCustomField.matches(t,
                List.of(new FilterCondition("cf:owner", "in", List.of("alice", "bob"))), "and", cfTypes));
    }

    @Test
    void comparatorSortsNumericWithNullsLast() {
        TaskVO a = task(1L, "x", Map.of("level", "10"));
        TaskVO b = task(2L, "x", Map.of("level", "2"));
        TaskVO c = task(3L, "x", Map.of()); // 无值 → 末尾
        List<TaskVO> list = new java.util.ArrayList<>(List.of(a, b, c));
        list.sort(TaskViewCustomField.comparator(List.of(new SortSpec("cf:level", "asc")), cfTypes));
        assertEquals(List.of(2L, 1L, 3L), list.stream().map(TaskVO::id).toList());
    }

    @Test
    void comparatorDescKeepsNullsLast() {
        TaskVO a = task(1L, "x", Map.of("level", "10"));
        TaskVO b = task(2L, "x", Map.of("level", "2"));
        TaskVO c = task(3L, "x", Map.of()); // 无值 → 降序也应末尾
        List<TaskVO> list = new java.util.ArrayList<>(List.of(c, a, b));
        list.sort(TaskViewCustomField.comparator(List.of(new SortSpec("cf:level", "desc")), cfTypes));
        assertEquals(List.of(1L, 2L, 3L), list.stream().map(TaskVO::id).toList());
    }

    @Test
    void referencedKeysCollectsFromColumnsFiltersSort() {
        ViewConfig config = new ViewConfig(null,
                List.of(new SortSpec("cf:due", "asc")), 1,
                new FilterGroup("and", List.of(new FilterCondition("cf:level", "ge", "1"))),
                List.of("title", "cf:owner"));
        assertEquals(java.util.Set.of("due", "level", "owner"),
                new java.util.HashSet<>(TaskViewCustomField.referencedKeys(config)));
    }
}
