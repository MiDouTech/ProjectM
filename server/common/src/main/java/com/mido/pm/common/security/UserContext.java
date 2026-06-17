package com.mido.pm.common.security;

/**
 * 当前用户上下文（ThreadLocal）。由认证过滤器（module-org Step 1-2）在请求入口写入，
 * 数据范围拦截器据此注入查询条件。无登录态时为空，拦截器不作处理。
 */
public final class UserContext {

    private static final ThreadLocal<CurrentUser> CURRENT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        CURRENT.set(user);
    }

    public static CurrentUser get() {
        return CURRENT.get();
    }

    /** 当前登录用户 id；无登录态返回 null。 */
    public static Long currentUserId() {
        CurrentUser user = CURRENT.get();
        return user == null ? null : user.getUserId();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
