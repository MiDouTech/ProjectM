package com.mido.pm.platform.support;

import com.mido.pm.common.tenant.TenantContext;

/**
 * 跨租户上下文作用域：平台域需以「某租户」身份读其业务数据（用量统计/模拟定位）时，
 * 临时把 {@link TenantContext} 切到目标租户，作用域结束自动还原，避免污染原上下文。
 *
 * <pre>{@code
 * try (PlatformTenantScope ignored = PlatformTenantScope.of(tenantId)) {
 *     // 此作用域内的业务查询按 tenantId 隔离
 * }
 * }</pre>
 */
public final class PlatformTenantScope implements AutoCloseable {

    private final Long previous;

    private PlatformTenantScope(Long tenantId) {
        this.previous = TenantContext.get();
        TenantContext.set(tenantId);
    }

    public static PlatformTenantScope of(Long tenantId) {
        return new PlatformTenantScope(tenantId);
    }

    @Override
    public void close() {
        if (previous == null) {
            TenantContext.clear();
        } else {
            TenantContext.set(previous);
        }
    }
}
