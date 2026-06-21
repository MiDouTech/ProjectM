package com.mido.pm.mcp.support;

import com.mido.pm.common.audit.AuditActions;
import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.common.security.ApiKeyContext;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * MCP 工具守卫：为每个工具调用统一施加「限流 → scope 校验 → 执行 → 调用审计」。
 *
 * <p>治理仅作用于携带 API Key（PAT）的调用（{@link ApiKeyContext} 非空）；JWT 登录用户直连放行。
 * scope 粗粒度两档：只读工具需 {@code mcp:read}，写工具需 {@code mcp:write}。</p>
 */
@Component
public class McpToolGuard {

    private static final Logger log = LoggerFactory.getLogger(McpToolGuard.class);

    private final AuditLogService auditLogService;
    private final McpRateLimiter rateLimiter;

    public McpToolGuard(AuditLogService auditLogService, McpRateLimiter rateLimiter) {
        this.auditLogService = auditLogService;
        this.rateLimiter = rateLimiter;
    }

    /** 包装只读工具（需 mcp:read 范围）。 */
    public SyncToolSpecification readOnly(Tool tool,
            BiFunction<McpSyncServerExchange, Map<String, Object>, CallToolResult> handler) {
        return guard(tool, ApiKeyContext.SCOPE_MCP_READ, handler);
    }

    /** 包装写工具（需 mcp:write 范围）。 */
    public SyncToolSpecification write(Tool tool,
            BiFunction<McpSyncServerExchange, Map<String, Object>, CallToolResult> handler) {
        return guard(tool, ApiKeyContext.SCOPE_MCP_WRITE, handler);
    }

    private SyncToolSpecification guard(Tool tool, String requiredScope,
            BiFunction<McpSyncServerExchange, Map<String, Object>, CallToolResult> handler) {
        return new SyncToolSpecification(tool, (exchange, args) -> {
            ApiKeyContext.Snapshot key = ApiKeyContext.get();
            if (key != null && !rateLimiter.tryAcquire(key.keyId())) {
                audit(tool, requiredScope, key, "rate_limited");
                return McpToolSupport.error("调用过于频繁，请稍后再试");
            }
            if (key != null && !key.hasScope(requiredScope)) {
                audit(tool, requiredScope, key, "denied_scope");
                return McpToolSupport.error("当前 API Key 缺少 " + requiredScope + " 范围，无权调用工具 " + tool.name());
            }
            CallToolResult result = handler.apply(exchange, args);
            audit(tool, requiredScope, key, Boolean.TRUE.equals(result.isError()) ? "error" : "ok");
            return result;
        });
    }

    /** 记录一次 MCP 工具调用审计（仅对 API Key 调用；审计失败不影响调用结果）。 */
    private void audit(Tool tool, String scope, ApiKeyContext.Snapshot key, String outcome) {
        if (key == null) {
            return;
        }
        try {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("tool", tool.name());
            detail.put("scope", scope);
            detail.put("outcome", outcome);
            detail.put("keyName", key.name() == null ? "" : key.name());
            auditLogService.record(AuditActions.TARGET_MCP, key.keyId(), AuditActions.MCP_INVOKE, detail);
        } catch (RuntimeException e) {
            log.warn("MCP 调用审计写入失败 tool={} keyId={}", tool.name(), key.keyId(), e);
        }
    }
}
