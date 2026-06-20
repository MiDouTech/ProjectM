package com.mido.pm.security;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 模拟登录只读拦截：当会话为平台运营的模拟登录（令牌携带 imp 声明）时，禁止一切写操作，
 * 仅放行安全方法（GET/HEAD/OPTIONS）。落实「模拟登录收敛为只读」，降低运营进租户排障的数据风险。
 */
@Component
public class ImpersonationReadOnlyInterceptor implements HandlerInterceptor {

    private static final Set<String> SAFE_METHODS = Set.of(
            HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.OPTIONS.name());

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        CurrentUser current = UserContext.get();
        if (current != null && current.isImpersonating() && !SAFE_METHODS.contains(request.getMethod())) {
            throw new BizException(ErrorCode.FORBIDDEN, "模拟登录为只读模式，禁止写操作");
        }
        return true;
    }
}
