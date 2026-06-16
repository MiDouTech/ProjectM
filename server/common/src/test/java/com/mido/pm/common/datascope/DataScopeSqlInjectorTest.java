package com.mido.pm.common.datascope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据范围 SQL 注入单测：验证条件以 AND 正确并入 WHERE，异常输入不破坏原 SQL。
 */
class DataScopeSqlInjectorTest {

    @Test
    void injectWhenNoWhere() {
        String out = DataScopeSqlInjector.inject("SELECT id FROM sys_user", "dept_id = 3");
        assertTrue(out.toUpperCase().contains("WHERE"), out);
        assertTrue(out.contains("dept_id = 3"), out);
    }

    @Test
    void injectAndsExistingWhere() {
        String out = DataScopeSqlInjector.inject(
                "SELECT id FROM sys_user WHERE status = 'active'", "dept_id IN (3, 4)");
        assertTrue(out.contains("status = 'active'"), out);
        assertTrue(out.contains("AND"), out);
        assertTrue(out.contains("dept_id IN (3, 4)"), out);
    }

    @Test
    void blankConditionKeepsSqlUnchanged() {
        String sql = "SELECT id FROM sys_user";
        assertEquals(sql, DataScopeSqlInjector.inject(sql, null));
        assertEquals(sql, DataScopeSqlInjector.inject(sql, "  "));
    }

    @Test
    void unparsableSqlReturnedAsIs() {
        String sql = "NOT A SELECT";
        assertEquals(sql, DataScopeSqlInjector.inject(sql, "dept_id = 3"));
    }
}
