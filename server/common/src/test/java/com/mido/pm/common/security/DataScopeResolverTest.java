package com.mido.pm.common.security;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 多角色数据范围合并单测：同资源取最宽。
 */
class DataScopeResolverTest {

    @Test
    void mergeTakesBroadestPerResource() {
        Map<String, DataScope> m = DataScopeResolver.mergeBroadest(List.of(
                new DataScopeResolver.RoleScope("user", "self"),
                new DataScopeResolver.RoleScope("user", "all"),
                new DataScopeResolver.RoleScope("project", "dept")));
        assertEquals(DataScope.ALL, m.get("user"), "self 与 all 取 all");
        assertEquals(DataScope.DEPT, m.get("project"));
    }

    @Test
    void nullOrEmptyYieldsEmpty() {
        assertTrue(DataScopeResolver.mergeBroadest(null).isEmpty());
        assertTrue(DataScopeResolver.mergeBroadest(List.of()).isEmpty());
    }
}
