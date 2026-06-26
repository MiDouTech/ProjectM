package com.mido.pm.common.security;

import com.mido.pm.common.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 字段级权限守卫单测：只读字段写入拒绝、未配置字段放行、无登录上下文放行、只读字段过滤。
 */
class FieldPermGuardTest {

    private final FieldPermGuard guard = new FieldPermGuard();

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private void login(Set<String> viewOnly) {
        CurrentUser u = new CurrentUser();
        u.setUserId(1L);
        u.setViewOnlyFields(viewOnly);
        UserContext.set(u);
    }

    @Test
    void rejectsEditOnViewOnlyField() {
        login(Set.of("task.priority"));
        assertThrows(BizException.class, () -> guard.assertEditable("task", "priority"));
    }

    @Test
    void allowsEditOnUnconfiguredField() {
        login(Set.of("task.priority"));
        assertDoesNotThrow(() -> guard.assertEditable("task", "title"));
    }

    @Test
    void allowsWhenNoLoginContext() {
        assertDoesNotThrow(() -> guard.assertEditable("task", "priority"));
    }

    @Test
    void viewOnlyFieldsStripsResourcePrefixAndFilters() {
        login(Set.of("task.priority", "task.status", "project.budget"));
        Set<String> taskViewOnly = guard.viewOnlyFields("task");
        assertEquals(Set.of("priority", "status"), taskViewOnly);
    }
}
