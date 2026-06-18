package com.mido.pm.view.dto;

import java.util.List;

/**
 * 视图查询配置（pm_view.config，结构锁定，不得自由扩展）。对齐 Worktile 视图设计器四步。
 *
 * <pre>
 * {
 *   "groupBy": "status",                       // 分组字段，可空（不分组）；白名单：status/assigneeId/priority/stage
 *   "sort": [ { "field": "dueDate", "dir": "asc" } ],   // 排序，多级；dir=asc|desc
 *   "expandLevel": 1,                          // 层级展开 1-5（子任务展开到 N 级，前端用）
 *   "filters": {
 *     "logic": "and",                          // and|or
 *     "conditions": [ { "field": "status", "op": "eq", "value": "进行中" } ]
 *   },
 *   "columns": [ "title", "status", "assigneeId", "dueDate" ]   // 展示列（前端用）
 * }
 * </pre>
 *
 * 字段/算子白名单由 module-task 的 ViewQueryTranslator 强制校验（禁任意列/算子，防注入）。
 */
public record ViewConfig(
        String groupBy,
        List<SortSpec> sort,
        Integer expandLevel,
        FilterGroup filters,
        List<String> columns) {

    /** 排序项：field 字段名，dir = asc|desc。 */
    public record SortSpec(String field, String dir) {
    }

    /** 筛选组：logic = and|or，作用于 conditions。 */
    public record FilterGroup(String logic, List<FilterCondition> conditions) {
    }

    /** 筛选条件：field 字段名，op 算子（eq/ne/gt/ge/lt/le/like/in/isNull/notNull），value 值。 */
    public record FilterCondition(String field, String op, Object value) {
    }
}
