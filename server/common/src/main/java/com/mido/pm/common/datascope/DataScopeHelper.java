package com.mido.pm.common.datascope;

import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.DataScope;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据范围条件构造（纯函数，可单测）。按用户与数据范围类型生成 SQL 条件片段。
 * 这是「数据范围注入逻辑」的核心，由单测覆盖全部 5 种 scope 与边界。
 */
public final class DataScopeHelper {

    /** 永真：不限制 */
    private static final String NO_RESTRICTION = null;
    /** 永假：无任何可见数据（缺失必要信息时的安全兜底，宁拒绝不泄露） */
    private static final String DENY = "1 = 0";

    private DataScopeHelper() {
    }

    /**
     * 构造数据范围条件片段。
     *
     * @return SQL 片段（如 {@code dept_id IN (1, 2)}）；返回 null 表示不施加限制（ALL）
     */
    public static String buildCondition(CurrentUser user, DataScope scope, String deptColumn, String userColumn) {
        if (user == null || scope == null) {
            return DENY;
        }
        return switch (scope) {
            case ALL -> NO_RESTRICTION;
            case SELF -> user.getUserId() == null ? DENY : userColumn + " = " + user.getUserId();
            case DEPT -> user.getDeptId() == null ? DENY : deptColumn + " = " + user.getDeptId();
            case DEPT_AND_SUB -> inClause(deptColumn, user.deptAndSubIds());
            case CUSTOM -> inClause(deptColumn, user.getCustomDeptIds());
        };
    }

    /** 生成 {@code column IN (a, b, c)}；空集合返回 DENY。 */
    private static String inClause(String column, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return DENY;
        }
        String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(", "));
        return column + " IN (" + joined + ")";
    }
}
