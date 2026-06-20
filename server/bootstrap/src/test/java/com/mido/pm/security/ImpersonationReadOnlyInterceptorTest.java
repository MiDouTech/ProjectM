package com.mido.pm.security;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

/** 模拟登录只读拦截单测：写操作在模拟态被拒，安全方法与非模拟态放行。 */
@ExtendWith(MockitoExtension.class)
class ImpersonationReadOnlyInterceptorTest {

    @Mock
    private HttpServletRequest request;

    private final ImpersonationReadOnlyInterceptor interceptor = new ImpersonationReadOnlyInterceptor();

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private void impersonating(boolean on) {
        CurrentUser u = new CurrentUser();
        u.setUserId(7L);
        if (on) {
            u.setImpersonatedBy(100L);
        }
        UserContext.set(u);
    }

    @Test
    void writeBlockedWhenImpersonating() {
        impersonating(true);
        lenient().when(request.getMethod()).thenReturn("POST");
        assertThrows(BizException.class, () -> interceptor.preHandle(request, null, null));
    }

    @Test
    void readAllowedWhenImpersonating() {
        impersonating(true);
        lenient().when(request.getMethod()).thenReturn("GET");
        assertTrue(interceptor.preHandle(request, null, null));
    }

    @Test
    void writeAllowedWhenNotImpersonating() {
        impersonating(false);
        lenient().when(request.getMethod()).thenReturn("POST");
        assertTrue(interceptor.preHandle(request, null, null));
    }

    @Test
    void noUserContextAllows() {
        UserContext.clear();
        lenient().when(request.getMethod()).thenReturn("DELETE");
        assertTrue(interceptor.preHandle(request, null, null));
    }
}
