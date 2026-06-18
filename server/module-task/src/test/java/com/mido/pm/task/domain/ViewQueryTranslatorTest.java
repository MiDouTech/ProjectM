package com.mido.pm.task.domain;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.task.entity.PmTask;
import com.mido.pm.view.dto.ViewConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 视图配置 → 查询条件转换单测：and/or 逻辑、排序、字段映射、白名单拦截。
 */
class ViewQueryTranslatorTest {

    private ViewConfig config(ViewConfig.FilterGroup filters, List<ViewConfig.SortSpec> sort) {
        return new ViewConfig(null, sort, 1, filters, List.of());
    }

    @Test
    void andLogicChainsConditions() {
        ViewConfig cfg = config(new ViewConfig.FilterGroup("and", List.of(
                new ViewConfig.FilterCondition("status", "eq", "进行中"),
                new ViewConfig.FilterCondition("priority", "ge", 2))), List.of());

        QueryWrapper<PmTask> qw = ViewQueryTranslator.build(cfg);

        String sql = qw.getSqlSegment();
        assertTrue(sql.contains("status ="), sql);
        assertTrue(sql.contains("priority >="), sql);
        assertTrue(sql.contains("AND"), sql);
        assertTrue(qw.getParamNameValuePairs().values().contains("进行中"));
    }

    @Test
    void orLogicJoinsWithOr() {
        ViewConfig cfg = config(new ViewConfig.FilterGroup("or", List.of(
                new ViewConfig.FilterCondition("status", "eq", "进行中"),
                new ViewConfig.FilterCondition("status", "eq", "已完成"))), List.of());

        String sql = ViewQueryTranslator.build(cfg).getSqlSegment();
        assertTrue(sql.contains("OR"), sql);
    }

    @Test
    void sortMapsFieldsAndDirection() {
        ViewConfig cfg = config(null, List.of(
                new ViewConfig.SortSpec("dueDate", "desc"),
                new ViewConfig.SortSpec("priority", "asc")));

        String sql = ViewQueryTranslator.build(cfg).getSqlSegment();
        assertTrue(sql.contains("ORDER BY"), sql);
        assertTrue(sql.contains("due_date DESC"), sql);
        assertTrue(sql.contains("priority ASC"), sql);
    }

    @Test
    void inOperatorUsesCollection() {
        ViewConfig cfg = config(new ViewConfig.FilterGroup("and", List.of(
                new ViewConfig.FilterCondition("status", "in", List.of("进行中", "已完成")))), List.of());

        String sql = ViewQueryTranslator.build(cfg).getSqlSegment();
        assertTrue(sql.contains("status IN"), sql);
    }

    @Test
    void unknownFieldRejected() {
        ViewConfig cfg = config(new ViewConfig.FilterGroup("and", List.of(
                new ViewConfig.FilterCondition("password", "eq", "x"))), List.of());
        assertThrows(BizException.class, () -> ViewQueryTranslator.build(cfg));
    }

    @Test
    void unknownOperatorRejected() {
        ViewConfig cfg = config(new ViewConfig.FilterGroup("and", List.of(
                new ViewConfig.FilterCondition("status", "drop", "x"))), List.of());
        assertThrows(BizException.class, () -> ViewQueryTranslator.build(cfg));
    }
}
