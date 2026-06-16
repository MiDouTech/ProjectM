package com.mido.pm.common.tenant;

/**
 * 当前请求的租户上下文（ThreadLocal）。
 * 由认证/网关拦截器在请求入口写入，MyBatis-Plus 多租户拦截器据此注入 tenant_id。
 * 业务代码禁止手写 tenant 条件，只通过本上下文传递。
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId) {
        CURRENT.set(tenantId);
    }

    public static Long get() {
        return CURRENT.get();
    }

    /** 取当前租户，无则返回默认值（用于系统级/未登录场景）。 */
    public static Long getOrDefault(Long defaultValue) {
        Long v = CURRENT.get();
        return v != null ? v : defaultValue;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
