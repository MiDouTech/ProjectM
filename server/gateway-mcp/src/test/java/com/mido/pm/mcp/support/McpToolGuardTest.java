package com.mido.pm.mcp.support;

import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.common.security.ApiKeyContext;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class McpToolGuardTest {

    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final Tool readTool = new Tool("t_read", "读", "{\"type\":\"object\"}");
    private final Tool writeTool = new Tool("t_write", "写", "{\"type\":\"object\"}");

    @AfterEach
    void tearDown() {
        ApiKeyContext.clear();
    }

    private McpToolGuard guard(McpRateLimiter limiter) {
        return new McpToolGuard(auditLogService, limiter);
    }

    private static BiFunction<McpSyncServerExchange, Map<String, Object>, CallToolResult> okHandler() {
        return (exchange, args) -> new CallToolResult("ok", false);
    }

    private static CallToolResult run(SyncToolSpecification spec) {
        return spec.call().apply(null, Map.of());
    }

    private static String text(CallToolResult r) {
        return ((TextContent) r.content().get(0)).text();
    }

    @Test
    void 无ApiKey上下文直接放行且不审计() {
        CallToolResult r = run(guard(id -> true).readOnly(readTool, okHandler()));
        assertThat(r.isError()).isFalse();
        verifyNoInteractions(auditLogService);
    }

    @Test
    void 只读key调用读工具放行并审计() {
        ApiKeyContext.set(new ApiKeyContext.Snapshot(1L, "只读连接器", Set.of("mcp:read")));
        CallToolResult r = run(guard(id -> true).readOnly(readTool, okHandler()));
        assertThat(r.isError()).isFalse();
        verify(auditLogService).record(eq("mcp"), eq(1L), eq("mcp_invoke"), any());
    }

    @Test
    void 只读key调用写工具被拒且不执行业务() {
        ApiKeyContext.set(new ApiKeyContext.Snapshot(1L, "只读连接器", Set.of("mcp:read")));
        AtomicBoolean ran = new AtomicBoolean(false);
        CallToolResult r = run(guard(id -> true).write(writeTool, (e, a) -> {
            ran.set(true);
            return new CallToolResult("ok", false);
        }));
        assertThat(r.isError()).isTrue();
        assertThat(text(r)).contains("mcp:write");
        assertThat(ran).isFalse();
        verify(auditLogService).record(eq("mcp"), eq(1L), eq("mcp_invoke"), any());
    }

    @Test
    void 限流超限返回错误且不执行业务() {
        ApiKeyContext.set(new ApiKeyContext.Snapshot(1L, "key", Set.of("mcp:write")));
        AtomicBoolean ran = new AtomicBoolean(false);
        CallToolResult r = run(guard(id -> false).write(writeTool, (e, a) -> {
            ran.set(true);
            return new CallToolResult("ok", false);
        }));
        assertThat(r.isError()).isTrue();
        assertThat(text(r)).contains("频繁");
        assertThat(ran).isFalse();
    }
}
