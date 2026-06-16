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

    /** 无需租户隔离的表（无 tenant_id 列或全局共享）。当前无，预留扩展。 */
    private static final Set<String> IGNORE_TABLES = Set.of();

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
