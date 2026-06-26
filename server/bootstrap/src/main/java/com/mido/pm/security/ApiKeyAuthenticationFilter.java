package com.mido.pm.security;

import com.mido.pm.common.security.ApiKeyContext;
import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.common.tenant.TenantContext;
import com.mido.pm.org.entity.SysApiKey;
import com.mido.pm.org.service.ApiKeyService;
import com.mido.pm.provider.identity.IdentityProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 开放平台 API Key 认证过滤器（租户链，置于 JWT 过滤器之前）：
 * 读 X-API-Key 头 → 解析为绑定用户 → 装配租户与用户上下文（等同该用户身份，继承其权限/数据范围）。
 * 未带或无效 key 则放行交由后续 JWT 过滤器处理。
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-API-Key";

    private final ApiKeyService apiKeyService;
    private final IdentityProvider identityProvider;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService, IdentityProvider identityProvider) {
        this.apiKeyService = apiKeyService;
        this.identityProvider = identityProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String rawKey = request.getHeader(HEADER);
            if (rawKey != null && !rawKey.isBlank()) {
                apiKeyService.resolve(rawKey).ifPresent(this::authenticate);
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
            ApiKeyContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(SysApiKey key) {
        // key 决定租户与用户：先切租户上下文，再在该租户内装配身份
        TenantContext.set(key.getTenantId());
        identityProvider.loadById(key.getUserId()).ifPresent(principal -> {
            List<SimpleGrantedAuthority> authorities = principal.getPermCodes().stream()
                    .map(SimpleGrantedAuthority::new).toList();
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            CurrentUser current = new CurrentUser();
            current.setUserId(principal.getUserId());
            current.setDeptId(principal.getDeptId());
            current.setSubDeptIds(principal.getSubDeptIds());
            current.setCustomDeptIds(principal.getCustomDeptIds());
            current.setResourceScopes(principal.getResourceScopes());
            current.setViewOnlyFields(principal.getViewOnlyFields());
            UserContext.set(current);
            ApiKeyContext.set(new ApiKeyContext.Snapshot(key.getId(), key.getName(), parseScopes(key.getScopes())));
            apiKeyService.touch(key.getId());
        });
    }

    /** 解析逗号分隔的 scopes；空/空白时宽松回退为读写两档，避免历史 key 误锁定。 */
    private Set<String> parseScopes(String scopes) {
        if (scopes == null || scopes.isBlank()) {
            return Set.of(ApiKeyContext.SCOPE_MCP_READ, ApiKeyContext.SCOPE_MCP_WRITE);
        }
        return Arrays.stream(scopes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
