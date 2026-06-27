package com.mido.pm.security;

import com.mido.pm.common.tenant.TenantDirectory;
import com.mido.pm.org.service.ApiKeyService;
import com.mido.pm.platform.security.PlatformTokenService;
import com.mido.pm.platform.service.PlatformAuthService;
import com.mido.pm.provider.identity.IdentityProvider;
import com.mido.pm.provider.sso.SsoProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 安全过滤链（无状态 JWT）。两条独立链按路径前缀分流：
 * <ul>
 *   <li>平台运营链（/api/v1/platform/**）：独立平台令牌 + {@link PlatformAuthenticationFilter}；</li>
 *   <li>租户应用链（其余）：租户 SSO 令牌 + {@link JwtAuthenticationFilter}。</li>
 * </ul>
 * 二者账号体系与密钥物理隔离。401/403 统一返回 docs/api-conventions.md 的 R 结构；
 * 方法级 @PreAuthorize 做权限码鉴权。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PERMIT_ALL = {
            "/api/v1/auth/**",
            "/api/v1/public/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error",
    };

    /** 平台运营链：仅匹配 /api/v1/platform/**，先于租户链。登录端点放行，其余需平台令牌。 */
    @Bean
    @Order(1)
    public SecurityFilterChain platformFilterChain(HttpSecurity http,
                                                   PlatformTokenService platformTokenService,
                                                   PlatformAuthService platformAuthService) throws Exception {
        PlatformAuthenticationFilter platformFilter =
                new PlatformAuthenticationFilter(platformTokenService, platformAuthService);
        http
                .securityMatcher("/api/v1/platform/**")
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers("/api/v1/platform/auth/login").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .addFilterBefore(platformFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 租户应用链：兜底匹配其余路径。 */
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SsoProvider ssoProvider,
                                                   IdentityProvider identityProvider,
                                                   ApiKeyService apiKeyService,
                                                   TenantDirectory tenantDirectory) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(ssoProvider, identityProvider, tenantDirectory);
        // 开放平台 API Key 过滤器置于 JWT 之前：带 X-API-Key 时以绑定用户身份认证，否则放行交给 JWT
        ApiKeyAuthenticationFilter apiKeyFilter = new ApiKeyAuthenticationFilter(apiKeyService, identityProvider);
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers(PERMIT_ALL).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    /** 未认证 → 401 + 错误码 40100。 */
    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) ->
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, 40100, "未认证或登录已过期");
    }

    /** 已认证但无权限 → 403 + 错误码 40300。 */
    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeJson(response, HttpServletResponse.SC_FORBIDDEN, 40300, "无权限");
    }

    private void writeJson(HttpServletResponse response, int status, int code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}";
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }
}
