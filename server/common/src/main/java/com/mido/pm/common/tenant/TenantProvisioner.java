package com.mido.pm.common.tenant;

/**
 * 租户开通播种 SPI：各业务域实现，为新建租户播种本域默认数据（角色/状态库/工作项类型/项目类型…）。
 * 由 platform 在租户创建事务内、切到新租户 {@link TenantContext} 后按 {@link #order()} 升序依次调用，
 * 使「开通即可用」。实现内禁手写 tenant 条件——insert 的 tenant_id 由多租户拦截器据 TenantContext 注入。
 *
 * <p>跨 provisioner 的 id 依赖（如 项目类型 绑定 审批流 id）经 {@link TenantProvisionContext#put}/{@code get}
 * 在共享袋中传递，不得跨域直接查表。
 */
public interface TenantProvisioner {

    /** 执行顺序（升序）。被依赖域排前：org(10) → approval(20) → task(25) → project(30)。 */
    int order();

    /** 为 {@code ctx.tenantId()} 指向的新租户播种本域默认数据（调用时 TenantContext 已切到该租户）。 */
    void provision(TenantProvisionContext ctx);
}
