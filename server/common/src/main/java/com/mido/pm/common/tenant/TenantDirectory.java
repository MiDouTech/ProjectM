package com.mido.pm.common.tenant;

/**
 * 租户目录端口：把「按租户编码解析租户/校验可登录」从平台域暴露给登录链路，
 * 避免 provider/org 直接依赖 module-platform（实现在 platform，注入按接口）。
 * 落地见 platform 的 PlatformTenantDirectory。
 */
public interface TenantDirectory {

    /** 按租户编码解析租户 ID；不存在返回 null。 */
    Long resolveIdByCode(String code);

    /** 租户是否存在且处于可登录状态（trial/active）；suspended/expired/closed 一律拒登。 */
    boolean isLoginable(Long tenantId);

    /** tenantCode 缺省时回落的自用租户 ID（阶段一对齐固定 tenant_id=1）。 */
    default Long defaultTenantId() {
        return TenantContext.DEFAULT_TENANT_ID;
    }
}
