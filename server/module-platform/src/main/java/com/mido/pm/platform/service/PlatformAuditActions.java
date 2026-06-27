package com.mido.pm.platform.service;

/** 平台运营审计动作码。集中登记，禁自造。 */
public final class PlatformAuditActions {

    public static final String TENANT_CREATED = "tenant_created";
    public static final String TENANT_UPDATED = "tenant_updated";
    public static final String TENANT_STATUS_CHANGED = "tenant_status_changed";
    public static final String TENANT_EXPIRED = "tenant_expired";
    public static final String TENANT_IMPERSONATED = "tenant_impersonated";
    public static final String TENANT_EXPORT_REQUESTED = "tenant_export_requested";
    public static final String TENANT_DELETION_REQUESTED = "tenant_deletion_requested";
    public static final String TENANT_DELETION_CANCELLED = "tenant_deletion_cancelled";
    public static final String TENANT_PURGED = "tenant_purged";
    public static final String TENANT_PURGE_SKIPPED = "tenant_purge_skipped";
    public static final String SUBSCRIPTION_SAVED = "subscription_saved";
    public static final String SUBSCRIPTION_OVERQUOTA = "subscription_overquota";
    public static final String PLAN_SAVED = "plan_saved";
    public static final String PLAN_DELETED = "plan_deleted";
    public static final String ADMIN_CREATED = "admin_created";
    public static final String ADMIN_UPDATED = "admin_updated";
    public static final String ADMIN_PASSWORD_RESET = "admin_password_reset";
    public static final String ADMIN_PASSWORD_CHANGED = "admin_password_changed";
    public static final String ADMIN_LOGIN = "admin_login";
    public static final String ADMIN_LOGIN_LOCKED = "admin_login_locked";
    public static final String REVENUE_SAVED = "revenue_saved";
    public static final String REVENUE_DELETED = "revenue_deleted";
    public static final String ANNOUNCEMENT_SAVED = "announcement_saved";
    public static final String PLAN_FEATURE_SAVED = "plan_feature_saved";

    /** 目标类型 */
    public static final String TARGET_TENANT = "tenant";
    public static final String TARGET_PLAN = "plan";
    public static final String TARGET_SUBSCRIPTION = "subscription";
    public static final String TARGET_ADMIN = "admin";
    public static final String TARGET_REVENUE = "revenue";
    public static final String TARGET_ANNOUNCEMENT = "announcement";

    private PlatformAuditActions() {
    }
}
