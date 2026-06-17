package com.mido.pm.provider.sso;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.provider.identity.IdentityProvider;
import com.mido.pm.provider.identity.UserPrincipal;
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
 * 本地 SSO 登录/签发/校验/刷新单测（mock IdentityProvider + 真实 BCrypt）。
 */
@ExtendWith(MockitoExtension.class)
class LocalSsoProviderTest {

    private static final String SECRET = "mido-pm-unit-test-secret-key-32bytes-long!!";

    @Mock
    private IdentityProvider identityProvider;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private LocalSsoProvider sso;

    @BeforeEach
    void setUp() {
        sso = new LocalSsoProvider(SECRET, 60_000L, identityProvider, encoder);
    }

    private UserPrincipal principal(String rawPwd, String status) {
        UserPrincipal p = new UserPrincipal();
        p.setUserId(7L);
        p.setUsername("admin");
        p.setPasswordHash(encoder.encode(rawPwd));
        p.setStatus(status);
        return p;
    }

    @Test
    void loginIssuesVerifiableToken() {
        lenient().when(identityProvider.loadByUsername("admin"))
                .thenReturn(Optional.of(principal("admin123", "active")));
        String token = sso.login("admin", "admin123");
        assertNotNull(token);
        assertEquals(7L, sso.verifyToken(token));
    }

    @Test
    void loginWrongPasswordThrows() {
        lenient().when(identityProvider.loadByUsername("admin"))
                .thenReturn(Optional.of(principal("admin123", "active")));
        assertThrows(BizException.class, () -> sso.login("admin", "wrong"));
    }

    @Test
    void loginUnknownUserThrows() {
        lenient().when(identityProvider.loadByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> sso.login("ghost", "x"));
    }

    @Test
    void loginDisabledUserThrows() {
        lenient().when(identityProvider.loadByUsername("admin"))
                .thenReturn(Optional.of(principal("admin123", "disabled")));
        assertThrows(BizException.class, () -> sso.login("admin", "admin123"));
    }

    @Test
    void verifyInvalidTokenReturnsNull() {
        assertNull(sso.verifyToken("not-a-jwt"));
    }

    @Test
    void refreshIssuesNewValidToken() {
        lenient().when(identityProvider.loadByUsername("admin"))
                .thenReturn(Optional.of(principal("admin123", "active")));
        String token = sso.login("admin", "admin123");
        String fresh = sso.refreshToken(token);
        assertNotNull(fresh);
        assertEquals(7L, sso.verifyToken(fresh));
    }

    @Test
    void weakSecretRejectedAtConstruction() {
        assertThrows(IllegalStateException.class,
                () -> new LocalSsoProvider("short", 60_000L, identityProvider, encoder));
    }
}
