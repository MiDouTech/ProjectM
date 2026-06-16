package com.mido.pm.common.datascope;

/**
 * 数据范围 opt-in 上下文（ThreadLocal）。
 * 服务在需要数据范围的查询前声明 {@link #set}，拦截器据此 + 当前用户范围改写 SQL，查询后 {@link #clear}。
 * 未声明则不注入任何数据范围条件——按需启用、显式可控。
 */
public final class DataScopeContext {

    /**
     * 数据范围设置。
     *
     * @param resource   资源标识（对应 sys_role_data_scope.resource）
     * @param deptColumn 目标表的部门列（dept/dept_and_sub/custom 据此过滤）
     * @param userColumn 目标表的归属人列（self 据此过滤，通常为 create_by）
     */
    public record Setting(String resource, String deptColumn, String userColumn) {
    }

    private static final ThreadLocal<Setting> CURRENT = new ThreadLocal<>();

    private DataScopeContext() {
    }

    public static void set(String resource, String deptColumn, String userColumn) {
        CURRENT.set(new Setting(resource, deptColumn, userColumn));
    }

    public static Setting get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
