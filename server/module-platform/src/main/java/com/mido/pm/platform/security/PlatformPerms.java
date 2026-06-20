package com.mido.pm.platform.security;

/**
 * 平台运营后台权限码集中登记（与租户侧 org:* 权限码两套体系，互不相通）。
 * 命名规约：platform:&lt;资源&gt;:&lt;动作&gt;。新增前在此查重。
 */
public final class PlatformPerms {

    /** 运营概览查看 */
    public static final String DASHBOARD_VIEW = "platform:dashboard:view";

    /** 租户查询 */
    public static final String TENANT_QUERY = "platform:tenant:query";
    /** 租户管理（开通/编辑/停用/启用/续期） */
    public static final String TENANT_MANAGE = "platform:tenant:manage";

    /** 套餐与配额查询 */
    public static final String PLAN_QUERY = "platform:plan:query";
    /** 套餐与配额管理 */
    public static final String PLAN_MANAGE = "platform:plan:manage";

    /** 租户订阅管理（绑定套餐/续期/变更） */
    public static final String SUBSCRIPTION_MANAGE = "platform:subscription:manage";

    /** 平台账号查询 */
    public static final String ADMIN_QUERY = "platform:admin:query";
    /** 平台账号与角色管理 */
    public static final String ADMIN_MANAGE = "platform:admin:manage";

    /** 运营审计查询 */
    public static final String AUDIT_QUERY = "platform:audit:query";

    private PlatformPerms() {
    }
}
