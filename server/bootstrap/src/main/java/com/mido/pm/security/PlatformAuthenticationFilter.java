package com.mido.pm.security;

import com.mido.pm.platform.security.PlatformContext;
import com.mido.pm.platform.security.PlatformPrincipal;
import com.mido.pm.platform.security.PlatformTokenService;
import com.mido.pm.platform.service.PlatformAuthService;
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
import java.util.Optional;

/**
 * 平台运营认证过滤器：仅作用于平台安全链（/api/v1/platform/**）。
 * 校验独立平台令牌 → 装配 {@link PlatformContext} 与 Spring Security 权限（权限码供 @PreAuthorize）。
 * 与租户侧 {@link JwtAuthenticationFilter} 互不影响：两条安全链按 securityMatcher 各管各的。
 */
public class PlatformAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final PlatformTokenService tokenService;
    private final PlatformAuthService authService;

    public PlatformAuthenticationFilter(PlatformTokenService tokenService, PlatformAuthService authService) {
        this.tokenService = tokenService;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (token != null) {
                Long adminId = tokenService.verify(token);
                if (adminId != null) {
                    Optional<PlatformPrincipal> principal = authService.loadPrincipal(adminId);
                    if (principal.isPresent()) {
                        authenticate(principal.get());
                        // 首登/被重置后强制改密：除自助改密与查询自身外，一律拦截，避免前端门控被绕过
                        if (principal.get().mustChangePassword() && !isChangePasswordAllowed(request)) {
                            writeForbidden(response);
                            return;
                        }
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            PlatformContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(PlatformPrincipal principal) {
        List<SimpleGrantedAuthority> authorities = principal.permCodes().stream()
                .map(SimpleGrantedAuthority::new).toList();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        PlatformContext.set(principal);
    }

    /** 强制改密期间放行的接口：自助改密、查询当前账号。 */
    private boolean isChangePasswordAllowed(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && (uri.endsWith("/platform/auth/password") || uri.endsWith("/platform/auth/me"));
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":40300,\"message\":\"请先修改初始密码后再操作\"}");
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            return header.substring(BEARER.length());
        }
        return null;
    }
}
