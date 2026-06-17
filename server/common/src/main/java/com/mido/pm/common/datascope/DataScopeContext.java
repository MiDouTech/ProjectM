package com.mido.pm.common.datascope;

import java.util.List;
import java.util.function.Supplier;

/**
 * 数据范围 opt-in 上下文（ThreadLocal）。
 * 服务在需要数据范围的查询前声明 {@link #set}，拦截器据此 + 当前用户范围改写 SQL，查询后 {@link #clear}。
 * 未声明则不注入任何数据范围条件——按需启用、显式可控。
 * 可选「成员可见性」并集：传 memberColumn + memberIds，则可见集 = 数据范围 ∪ 我参与的对象（成员 ACL 轴）。
 */
public final class DataScopeContext {

    /**
     * 数据范围设置。
     *
     * @param resource     资源标识（对应 sys_role_data_scope.resource）
     * @param deptColumn   目标表的部门列（dept/dept_and_sub/custom 据此过滤）
     * @param userColumn   目标表的归属人列（self 据此过滤，通常为 create_by）
     * @param memberColumn 成员并集匹配列（项目=id，任务=project_id）；为空则不加成员并集
     * @param memberIds    我参与的对象 id 集（成员 ACL 轴）；为空则不加成员并集
     */
    public record Setting(String resource, String deptColumn, String userColumn,
                          String memberColumn, List<Long> memberIds) {
    }

    private static final ThreadLocal<Setting> CURRENT = new ThreadLocal<>();

    private DataScopeContext() {
    }

    public static void set(String resource, String deptColumn, String userColumn) {
        CURRENT.set(new Setting(resource, deptColumn, userColumn, null, null));
    }

    public static Setting get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** 纯数据范围查询封装（自动 set + finally clear）。 */
    public static <T> T scoped(String resource, String deptColumn, String userColumn, Supplier<T> action) {
        return scoped(resource, deptColumn, userColumn, null, null, action);
    }

    /**
     * 带「成员可见性」并集的查询封装：可见集 = 数据范围 ∪ (memberColumn IN memberIds)。
     * 自动 set + finally clear，杜绝 ThreadLocal 串号。
     */
    public static <T> T scoped(String resource, String deptColumn, String userColumn,
                               String memberColumn, List<Long> memberIds, Supplier<T> action) {
        CURRENT.set(new Setting(resource, deptColumn, userColumn, memberColumn, memberIds));
        try {
            return action.get();
        } finally {
            clear();
        }
    }
}
