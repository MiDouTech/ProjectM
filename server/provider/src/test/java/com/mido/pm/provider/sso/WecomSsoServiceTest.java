package com.mido.pm.provider.sso;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.provider.message.WecomBinding;
import com.mido.pm.provider.message.WecomUserResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 企微 SSO 编排单测：未启用拒绝、未绑定拒绝、绑定则签发令牌。 */
@ExtendWith(MockitoExtension.class)
class WecomSsoServiceTest {

    @Mock private WecomSsoClient ssoClient;
    @Mock private WecomUserResolver userResolver;
    @Mock private SsoProvider ssoProvider;
    @InjectMocks private WecomSsoService service;

    @Test
    void disabledRejectsLogin() {
        when(ssoClient.enabled()).thenReturn(false);
        assertThrows(BizException.class, () -> service.loginByCode("CODE"));
    }

    @Test
    void unboundUserRejected() {
        when(ssoClient.enabled()).thenReturn(true);
        when(ssoClient.userIdByCode("CODE")).thenReturn("zhangsan");
        when(userResolver.bindingByExternalId("zhangsan")).thenReturn(null);
        assertThrows(BizException.class, () -> service.loginByCode("CODE"));
    }

    @Test
    void authFailureRejected() {
        when(ssoClient.enabled()).thenReturn(true);
        when(ssoClient.userIdByCode("CODE")).thenReturn(null);
        assertThrows(BizException.class, () -> service.loginByCode("CODE"));
    }

    @Test
    void boundUserGetsToken() {
        when(ssoClient.enabled()).thenReturn(true);
        when(ssoClient.userIdByCode("CODE")).thenReturn("zhangsan");
        when(userResolver.bindingByExternalId("zhangsan")).thenReturn(new WecomBinding(100L, 1L));
        when(ssoProvider.issueAccessToken(100L, 1L)).thenReturn("jwt-token");

        assertEquals("jwt-token", service.loginByCode("CODE"));
        verify(ssoProvider).issueAccessToken(100L, 1L);
    }

    @Test
    void authorizeUrlRequiresEnabled() {
        lenient().when(ssoClient.enabled()).thenReturn(false);
        assertThrows(BizException.class, () -> service.authorizeUrl("https://cb", "s"));
    }
}
