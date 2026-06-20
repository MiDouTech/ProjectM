package com.mido.pm.common.tenant;

/**
 * 租户用户定位端口：供平台域「模拟登录」定位某租户的目标用户，
 * 避免 platform 直接查 sys_user（跨域直查违规）。实现在 module-org。
 */
public interface TenantUserLocator {

    /**
     * 取某租户的主用户 ID（用于模拟登录的目标身份）：
     * 优先该租户管理员，否则该租户最早的 active 用户；无可用用户返回 null。
     *
     * @param tenantId 目标租户 ID
     */
    Long primaryUserId(Long tenantId);
}
