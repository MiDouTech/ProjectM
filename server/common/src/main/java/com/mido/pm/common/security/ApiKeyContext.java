package com.mido.pm.common.security;

import java.util.Set;

/**
 * 当前 API Key 上下文（ThreadLocal）。携带 X-API-Key（PAT）调用时，由认证过滤器在请求入口写入，
 * 供接入层（如 MCP 连接器）做调用范围（scope）校验、限流与调用审计。
 *
 * <p>无 API Key 调用态（如 JWT 登录用户）为空，接入层据此放行（不做 scope 约束）。</p>
 */
public final class ApiKeyContext {

    /** MCP 只读范围。 */
    public static final String SCOPE_MCP_READ = "mcp:read";
    /** MCP 读写范围。 */
    public static final String SCOPE_MCP_WRITE = "mcp:write";

    private static final ThreadLocal<Snapshot> CURRENT = new ThreadLocal<>();

    private ApiKeyContext() {
    }

    public static void set(Snapshot snapshot) {
        CURRENT.set(snapshot);
    }

    /** 当前 API Key 快照；无 API Key 调用态返回 null。 */
    public static Snapshot get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    /**
     * API Key 快照。
     *
     * @param keyId  key 主键（用于限流与审计定位）
     * @param name   key 名称（审计展示）
     * @param scopes 授予的调用范围集合
     */
    public record Snapshot(Long keyId, String name, Set<String> scopes) {

        /** 是否具备指定范围。 */
        public boolean hasScope(String scope) {
            return scopes != null && scopes.contains(scope);
        }
    }
}
