package com.mido.pm.platform.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.tenant.TenantUserLocator;
import com.mido.pm.platform.dto.ImpersonateVO;
import com.mido.pm.platform.entity.SysTenant;
import com.mido.pm.platform.mapper.SysTenantMapper;
import com.mido.pm.platform.security.PlatformContext;
import com.mido.pm.platform.security.PlatformPrincipal;
import com.mido.pm.provider.sso.SsoProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 模拟登录服务单测：目标用户解析、令牌签发、审计、异常分支。 */
@ExtendWith(MockitoExtension.class)
class PlatformImpersonationServiceTest {

    @Mock
    private SysTenantMapper tenantMapper;
    @Mock
    private TenantUserLocator tenantUserLocator;
    @Mock
    private SsoProvider ssoProvider;
    @Mock
    private PlatformAuditService auditService;

    private PlatformImpersonationService service() {
        return new PlatformImpersonationService(tenantMapper, tenantUserLocator, ssoProvider,
                auditService, 1_800_000L);
    }

    private SysTenant tenant(Long adminUserId) {
        SysTenant t = new SysTenant();
        t.setId(9L);
        t.setCode("acme");
        t.setStatus("active");
        t.setAdminUserId(adminUserId);
        return t;
    }

    @AfterEach
    void tearDown() {
        PlatformContext.clear();
    }

    @Test
    void impersonatePrefersTenantAdminUser() {
        PlatformContext.set(new PlatformPrincipal(100L, "superadmin", "超管", List.of(), false));
        when(tenantMapper.selectById(9L)).thenReturn(tenant(5L));
        when(ssoProvider.issueImpersonationToken(eq(5L), eq(9L), eq(100L))).thenReturn("tok");

        ImpersonateVO vo = service().impersonate(9L);

        assertEquals("tok", vo.token());
        assertEquals(5L, vo.targetUserId());
        assertEquals("acme", vo.tenantCode());
        verify(auditService).record(any(), any(), eq(9L), any());
    }

    @Test
    void impersonateFallsBackToPrimaryUser() {
        PlatformContext.set(new PlatformPrincipal(100L, "superadmin", "超管", List.of(), false));
        when(tenantMapper.selectById(9L)).thenReturn(tenant(null));
        when(tenantUserLocator.primaryUserId(9L)).thenReturn(8L);
        when(ssoProvider.issueImpersonationToken(eq(8L), eq(9L), eq(100L))).thenReturn("tok");

        assertEquals(8L, service().impersonate(9L).targetUserId());
    }

    @Test
    void impersonateUnknownTenantThrows() {
        when(tenantMapper.selectById(9L)).thenReturn(null);
        assertThrows(BizException.class, () -> service().impersonate(9L));
    }

    @Test
    void impersonateNoUserThrows() {
        when(tenantMapper.selectById(9L)).thenReturn(tenant(null));
        when(tenantUserLocator.primaryUserId(9L)).thenReturn(null);
        lenient().when(ssoProvider.issueImpersonationToken(any(), any(), any())).thenReturn("tok");
        assertThrows(BizException.class, () -> service().impersonate(9L));
    }
}
