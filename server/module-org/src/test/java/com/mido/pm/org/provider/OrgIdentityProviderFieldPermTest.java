package com.mido.pm.org.provider;

import com.mido.pm.common.security.FieldAccess;
import com.mido.pm.org.entity.SysFieldPerm;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 字段权限多角色合并（取最宽）单测：未配置该字段的角色默认授予 edit。
 */
class OrgIdentityProviderFieldPermTest {

    private SysFieldPerm fp(long roleId, String resource, String field, String access) {
        SysFieldPerm f = new SysFieldPerm();
        f.setRoleId(roleId);
        f.setResource(resource);
        f.setField(field);
        f.setAccess(access);
        return f;
    }

    @Test
    void singleRoleViewIsViewOnly() {
        Set<String> v = OrgIdentityProvider.mergeViewOnly(
                List.of(fp(1, "task", "priority", FieldAccess.VIEW)), 1);
        assertTrue(v.contains("task.priority"));
    }

    @Test
    void anyRoleEditMakesEditable() {
        Set<String> v = OrgIdentityProvider.mergeViewOnly(List.of(
                fp(1, "task", "priority", FieldAccess.VIEW),
                fp(2, "task", "priority", FieldAccess.EDIT)), 2);
        assertFalse(v.contains("task.priority"));
    }

    @Test
    void unconfiguredRoleGrantsEditByDefault() {
        // 用户有 2 个角色：role1 限制 view，role2 未配置该字段 → 应可编辑（最宽）
        Set<String> v = OrgIdentityProvider.mergeViewOnly(
                List.of(fp(1, "task", "priority", FieldAccess.VIEW)), 2);
        assertFalse(v.contains("task.priority"));
    }

    @Test
    void allRolesViewIsViewOnly() {
        Set<String> v = OrgIdentityProvider.mergeViewOnly(List.of(
                fp(1, "task", "priority", FieldAccess.VIEW),
                fp(2, "task", "priority", FieldAccess.VIEW)), 2);
        assertEquals(Set.of("task.priority"), v);
    }
}
