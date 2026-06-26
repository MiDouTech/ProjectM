package com.mido.pm.security;

import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
import com.mido.pm.common.tenant.TenantContext;
import com.mido.pm.provider.identity.IdentityProvider;
import com.mido.pm.provider.identity.UserPrincipal;
import com.mido.pm.provider.sso.SsoProvider;
import com.mido.pm.provider.sso.TokenPayload;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器（组合根）：校验 Bearer 令牌 → 装配安全上下文与 {@link UserContext}。
 * 同时使用 {@link SsoProvider} 与 {@link IdentityProvider}，故置于 bootstrap。
 * 权限码作为 Spring Security 权限，供 {@code @PreAuthorize} 鉴权；数据范围写入 UserContext 供拦截器使用。
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final SsoProvider ssoProvider;
    private final IdentityProvider identityProvider;

    public JwtAuthenticationFilter(SsoProvider ssoProvider, IdentityProvider identityProvider) {
        this.ssoProvider = ssoProvider;
        this.identityProvider = identityProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 若前置 API Key 过滤器已认证，则不再用 JWT 覆盖
            String token = resolveToken(request);
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                TokenPayload payload = ssoProvider.verifyToken(token);
                if (payload != null) {
                    // 按令牌真实租户覆盖基线租户上下文，落地多租户隔离（替代固定 tenant_id=1）
                    TenantContext.set(payload.tenantId());
                    Long impersonatedBy = payload.impersonatedBy();
                    identityProvider.loadById(payload.userId())
                            .ifPresent(p -> authenticate(p, impersonatedBy));
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(UserPrincipal principal, Long impersonatedBy) {
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
        current.setImpersonatedBy(impersonatedBy);
        UserContext.set(current);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            return header.substring(BEARER.length());
        }
        return null;
    }
}
