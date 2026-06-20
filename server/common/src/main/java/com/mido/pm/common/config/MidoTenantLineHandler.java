package com.mido.pm.common.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.mido.pm.common.tenant.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.Set;

/**
 * 多租户处理器：为业务 SQL 自动注入 tenant_id 条件（见 docs/data-model.md / api-conventions.md）。
 * 租户值取自 {@link TenantContext}，业务代码禁止手写 tenant 条件。
 */
public class MidoTenantLineHandler implements TenantLineHandler {

    /**
     * 无需租户隔离的表：平台域（SaaS 运营总后台）全局表，无 tenant_id 列、跨租户共享。
     * 这是对「所有业务表必带 tenant_id」的正式架构例外（见 com.mido.pm.platform 包说明）。
     * 注：sys_tenant_subscription 含 tenant_id 列，但那是指向 sys_tenant.id 的普通引用列、非隔离列，
     * 同样需忽略以免被自动注入 where 条件。
     */
    private static final Set<String> IGNORE_TABLES = Set.of(
            "sys_tenant",
            "sys_plan",
            "sys_plan_quota",
            "sys_tenant_subscription",
            "sys_tenant_quota_usage",
            "sys_revenue_record",
            "sys_announcement",
            "sys_plan_feature",
            "sys_platform_admin",
            "sys_platform_role",
            "sys_platform_admin_role",
            "sys_platform_role_perm",
            "sys_platform_audit_log");

    @Override
    public Expression getTenantId() {
        // 取请求级 TenantContext；缺省回落固定单租户，避免空指针。
        return new LongValue(TenantContext.getOrDefault(TenantContext.DEFAULT_TENANT_ID));
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return IGNORE_TABLES.contains(tableName.toLowerCase());
    }
}
