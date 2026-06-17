package com.mido.pm.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 多租户上下文地基：在每个请求入口写入 {@link TenantContext}，请求结束清理 ThreadLocal。
 *
 * <p>阶段一为单租户，统一写入 {@link TenantContext#DEFAULT_TENANT_ID}；Step 1 认证落地后，
 * 改为从 JWT 解析真实租户写入。有了这层地基，各业务模块无需各自处理租户——
 * MyBatis-Plus 多租户拦截器会据此自动注入 tenant_id。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // TODO Step 1：改为从 JWT 解析租户；当前阶段单租户固定。
            TenantContext.set(TenantContext.DEFAULT_TENANT_ID);
            filterChain.doFilter(request, response);
        } finally {
            // 必须清理，防止线程池复用导致租户串号 / ThreadLocal 泄漏。
            TenantContext.clear();
        }
    }
}
