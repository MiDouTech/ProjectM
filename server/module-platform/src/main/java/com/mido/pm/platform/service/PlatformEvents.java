package com.mido.pm.platform.service;

/** 平台/租户域领域事件名（取自 docs/domain-events.md §5.4，禁自造）。 */
public final class PlatformEvents {

    public static final String TENANT_REGISTERED = "tenant.registered";
    public static final String TENANT_SUBSCRIPTION_CHANGED = "tenant.subscription_changed";
    public static final String TENANT_STATUS_CHANGED = "tenant.status_changed";
    public static final String TENANT_EXPIRED = "tenant.expired";
    public static final String TENANT_DELETION_REQUESTED = "tenant.deletion_requested";
    public static final String TENANT_PURGED = "tenant.purged";

    private PlatformEvents() {
    }
}
