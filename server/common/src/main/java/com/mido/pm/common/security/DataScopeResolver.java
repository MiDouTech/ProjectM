package com.mido.pm.common.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据范围合并（纯函数，可单测）：一个用户多角色时，按资源取权限最宽的范围。
 * 例：user 资源同时有 self 与 all → 取 all。
 */
public final class DataScopeResolver {

    /** 角色数据范围条目（资源标识 + scope 编码）。 */
    public record RoleScope(String resource, String code) {
    }

    private DataScopeResolver() {
    }

    public static Map<String, DataScope> mergeBroadest(List<RoleScope> scopes) {
        Map<String, DataScope> merged = new HashMap<>();
        if (scopes == null) {
            return merged;
        }
        for (RoleScope rs : scopes) {
            DataScope s = DataScope.fromCode(rs.code(), DataScope.SELF);
            merged.merge(rs.resource(), s, (a, b) -> a.breadth() >= b.breadth() ? a : b);
        }
        return merged;
    }
}
