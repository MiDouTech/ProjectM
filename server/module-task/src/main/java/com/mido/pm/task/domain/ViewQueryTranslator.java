package com.mido.pm.task.domain;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.view.dto.ViewConfig;

import java.util.Collection;
import java.util.Map;

/**
 * 视图配置 → 任务查询条件转换（纯函数）。字段/算子走白名单（禁任意列/算子，防注入）。
 * 用列名字符串构建 QueryWrapper（非 Lambda）：filters→WHERE、sort→ORDER BY；groupBy 由服务层在内存分组。
 */
public final class ViewQueryTranslator {

    /** 视图字段 → 数据库列（白名单）。 */
    private static final Map<String, String> COLUMN = Map.of(
            "status", "status",
            "assigneeId", "assignee_id",
            "priority", "priority",
            "stage", "stage",
            "startDate", "start_date",
            "dueDate", "due_date",
            "isMilestone", "is_milestone",
            "title", "title",
            "parentId", "parent_id");

    private ViewQueryTranslator() {
    }

    /** 是否允许作为分组/排序/筛选字段。 */
    public static String column(String field) {
        String col = COLUMN.get(field);
        if (col == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法视图字段: " + field);
        }
        return col;
    }

    /** config → QueryWrapper（不含 project/tenant，由服务/拦截器追加）。 */
    public static QueryWrapper<PmTask> build(ViewConfig config) {
        QueryWrapper<PmTask> qw = new QueryWrapper<>();
        if (config == null) {
            return qw;
        }
        applyFilters(qw, config.filters());
        applySort(qw, config.sort());
        return qw;
    }

    private static void applyFilters(QueryWrapper<PmTask> qw, ViewConfig.FilterGroup filters) {
        if (filters == null || filters.conditions() == null || filters.conditions().isEmpty()) {
            return;
        }
        boolean or = "or".equalsIgnoreCase(filters.logic());
        qw.and(w -> {
            boolean first = true;
            for (ViewConfig.FilterCondition c : filters.conditions()) {
                if (!first && or) {
                    w.or();
                }
                applyOp(w, column(c.field()), c.op(), c.value());
                first = false;
            }
        });
    }

    private static void applyOp(QueryWrapper<PmTask> w, String col, String op, Object value) {
        switch (op == null ? "" : op) {
            case "eq" -> w.eq(col, value);
            case "ne" -> w.ne(col, value);
            case "gt" -> w.gt(col, value);
            case "ge" -> w.ge(col, value);
            case "lt" -> w.lt(col, value);
            case "le" -> w.le(col, value);
            case "like" -> w.like(col, value);
            case "in" -> {
                if (!(value instanceof Collection<?> coll)) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "in 算子值须为数组: " + col);
                }
                w.in(col, coll);
            }
            case "isNull" -> w.isNull(col);
            case "notNull" -> w.isNotNull(col);
            default -> throw new BizException(ErrorCode.PARAM_ERROR, "非法算子: " + op);
        }
    }

    private static void applySort(QueryWrapper<PmTask> qw, java.util.List<ViewConfig.SortSpec> sort) {
        if (sort == null) {
            return;
        }
        for (ViewConfig.SortSpec s : sort) {
            qw.orderBy(true, !"desc".equalsIgnoreCase(s.dir()), column(s.field()));
        }
    }
}
