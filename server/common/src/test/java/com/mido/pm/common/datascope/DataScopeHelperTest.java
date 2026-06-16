package com.mido.pm.common.datascope;

import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.DataScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 数据范围注入逻辑单测：覆盖 self/dept/dept_and_sub/all/custom 与边界。
 */
class DataScopeHelperTest {

    private CurrentUser user(Long userId, Long deptId, List<Long> sub, List<Long> custom) {
        CurrentUser u = new CurrentUser();
        u.setUserId(userId);
        u.setDeptId(deptId);
        u.setSubDeptIds(sub);
        u.setCustomDeptIds(custom);
        return u;
    }

    @Test
    void all_noRestriction() {
        String c = DataScopeHelper.buildCondition(user(7L, 3L, List.of(), List.of()),
                DataScope.ALL, "dept_id", "create_by");
        assertNull(c, "ALL 不应施加任何条件");
    }

    @Test
    void self_byUser() {
        String c = DataScopeHelper.buildCondition(user(7L, 3L, List.of(), List.of()),
                DataScope.SELF, "dept_id", "create_by");
        assertEquals("create_by = 7", c);
    }

    @Test
    void dept_byDept() {
        String c = DataScopeHelper.buildCondition(user(7L, 3L, List.of(), List.of()),
                DataScope.DEPT, "dept_id", "create_by");
        assertEquals("dept_id = 3", c);
    }

    @Test
    void deptAndSub_inClause() {
        String c = DataScopeHelper.buildCondition(user(7L, 3L, List.of(4L, 5L), List.of()),
                DataScope.DEPT_AND_SUB, "dept_id", "create_by");
        assertEquals("dept_id IN (3, 4, 5)", c);
    }

    @Test
    void custom_inClause() {
        String c = DataScopeHelper.buildCondition(user(7L, 3L, List.of(), List.of(8L, 9L)),
                DataScope.CUSTOM, "dept_id", "create_by");
        assertEquals("dept_id IN (8, 9)", c);
    }

    @Test
    void custom_emptyDenies() {
        String c = DataScopeHelper.buildCondition(user(7L, 3L, List.of(), List.of()),
                DataScope.CUSTOM, "dept_id", "create_by");
        assertEquals("1 = 0", c);
    }

    @Test
    void self_nullUserDenies() {
        String c = DataScopeHelper.buildCondition(user(null, 3L, List.of(), List.of()),
                DataScope.SELF, "dept_id", "create_by");
        assertEquals("1 = 0", c);
    }

    @Test
    void dept_nullDeptDenies() {
        String c = DataScopeHelper.buildCondition(user(7L, null, List.of(), List.of()),
                DataScope.DEPT, "dept_id", "create_by");
        assertEquals("1 = 0", c);
    }

    @Test
    void nullUserDenies() {
        String c = DataScopeHelper.buildCondition(null, DataScope.ALL, "dept_id", "create_by");
        assertEquals("1 = 0", c);
    }
}
