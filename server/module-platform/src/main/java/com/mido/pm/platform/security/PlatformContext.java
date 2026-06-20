package com.mido.pm.platform.security;

/**
 * 平台运营人员上下文（ThreadLocal）。由平台认证过滤器在 {@code /api/v1/platform/**} 请求入口写入，
 * 请求结束清理。与租户侧 {@code UserContext} 相互独立，避免运营态与租户态串号。
 */
public final class PlatformContext {

    private static final ThreadLocal<PlatformPrincipal> CURRENT = new ThreadLocal<>();

    private PlatformContext() {
    }

    public static void set(PlatformPrincipal principal) {
        CURRENT.set(principal);
    }

    public static PlatformPrincipal get() {
        return CURRENT.get();
    }

    /** 当前运营账号 ID；无登录态返回 null。 */
    public static Long currentAdminId() {
        PlatformPrincipal p = CURRENT.get();
        return p == null ? null : p.adminId();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
