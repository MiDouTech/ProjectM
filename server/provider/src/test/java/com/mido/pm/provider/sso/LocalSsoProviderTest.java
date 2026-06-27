package com.mido.pm.provider.sso;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.tenant.TenantContext;
import com.mido.pm.common.tenant.TenantDirectory;
import com.mido.pm.provider.identity.IdentityProvider;
import com.mido.pm.provider.identity.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

/**
 * 本地 SSO 登录/签发/校验/刷新单测（mock IdentityProvider + TenantDirectory + 真实 BCrypt）。
 * 覆盖多租户登录：令牌携带租户声明、缺省回落自用租户、模拟登录令牌。
 */
@ExtendWith(MockitoExtension.class)
class LocalSsoProviderTest {

    private static final String SECRET = "mido-pm-unit-test-secret-key-32bytes-long!!";

    @Mock
    private IdentityProvider identityProvider;
    @Mock
    private TenantDirectory tenantDirectory;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private LocalSsoProvider sso;

    @BeforeEach
    void setUp() {
        sso = new LocalSsoProvider(SECRET, 60_000L, 30_000L, identityProvider, encoder, tenantDirectory);
        lenient().when(tenantDirectory.defaultTenantId()).thenReturn(1L);
        lenient().when(tenantDirectory.isLoginable(1L)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private UserPrincipal principal(String rawPwd, String status) {
        UserPrincipal p = new UserPrincipal();
        p.setUserId(7L);
        p.setTenantId(1L);
        p.setUsername("admin");
        p.setPasswordHash(encoder.encode(rawPwd));
        p.setStatus(status);
        return p;
    }

    @Test
    void loginIssuesTokenCarryingUserAndTenant() {
        lenient().when(identityProvider.loadByUsername("admin"))
                .thenReturn(Optional.of(principal("admin123", "active")));
        String token = sso.login("admin", "admin123", null);
        assertNotNull(token);
        TokenPayload payload = sso.verifyToken(token);
        assertEquals(7L, payload.userId());
        assertEquals(1L, payload.tenantId());
        assertNull(payload.impersonatedBy());
    }

    @Test
    void loginByPhoneIssuesVerifiableToken() {
        lenient().when(identityProvider.loadByPhone("13800138000"))
                .thenReturn(Optional.of(principal("pass123", "active")));
        String token = sso.login("13800138000", "pass123", null);
        assertEquals(7L, sso.verifyToken(token).userId());
    }

    @Test
    void loginResolvesTenantByCode() {
        lenient().when(tenantDirectory.resolveIdByCode("acme")).thenReturn(9L);
        lenient().when(tenantDirectory.isLoginable(9L)).thenReturn(true);
        lenient().when(identityProvider.loadByUsername("admin"))
                .thenReturn(Optional.of(principal("admin123", "active")));
        String token = sso.login("admin", "admin123", "acme");
        assertEquals(9L, sso.verifyToken(token).tenantId());
    }

    @Test
    void loginUnknownTenantCodeThrows() {
        lenient().when(tenantDirectory.resolveIdByCode("ghost")).thenReturn(null);
        assertThrows(BizException.class, () -> sso.login("admin", "x", "ghost"));
    }

    @Test
    void loginWrongPasswordThrows() {
        lenient().when(identityProvider.loadByUsername("admin"))
                .thenReturn(Optional.of(principal("admin123", "active")));
        assertThrows(BizException.class, () -> sso.login("admin", "wrong", null));
    }

    @Test
    void loginUnknownUserThrows() {
        lenient().when(identityProvider.loadByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> sso.login("ghost", "x", null));
    }

    @Test
    void loginDisabledUserThrows() {
        lenient().when(identityProvider.loadByUsername("admin"))
                .thenReturn(Optional.of(principal("admin123", "disabled")));
        assertThrows(BizException.class, () -> sso.login("admin", "admin123", null));
    }

    @Test
    void verifyInvalidTokenReturnsNull() {
        assertNull(sso.verifyToken("not-a-jwt"));
    }

    @Test
    void refreshIssuesNewValidTokenPreservingTenant() {
        lenient().when(identityProvider.loadByUsername("admin"))
                .thenReturn(Optional.of(principal("admin123", "active")));
        String token = sso.login("admin", "admin123", null);
        String fresh = sso.refreshToken(token);
        assertNotNull(fresh);
        assertEquals(1L, sso.verifyToken(fresh).tenantId());
    }

    @Test
    void impersonationTokenCarriesImpersonator() {
        String token = sso.issueImpersonationToken(7L, 9L, 100L);
        TokenPayload payload = sso.verifyToken(token);
        assertEquals(7L, payload.userId());
        assertEquals(9L, payload.tenantId());
        assertEquals(100L, payload.impersonatedBy());
    }

    @Test
    void refreshRejectsImpersonationToken() {
        String token = sso.issueImpersonationToken(7L, 9L, 100L);
        // 模拟登录令牌为短时令牌，不允许刷新延长
        assertNull(sso.refreshToken(token));
    }

    @Test
    void weakSecretRejectedAtConstruction() {
        assertThrows(IllegalStateException.class,
                () -> new LocalSsoProvider("short", 60_000L, 30_000L, identityProvider, encoder, tenantDirectory));
    }
}
