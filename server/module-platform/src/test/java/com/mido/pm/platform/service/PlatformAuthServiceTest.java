package com.mido.pm.platform.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.platform.entity.SysPlatformAdmin;
import com.mido.pm.platform.entity.SysPlatformAdminRole;
import com.mido.pm.platform.entity.SysPlatformRolePerm;
import com.mido.pm.platform.mapper.SysPlatformAdminMapper;
import com.mido.pm.platform.mapper.SysPlatformAdminRoleMapper;
import com.mido.pm.platform.mapper.SysPlatformRolePermMapper;
import com.mido.pm.platform.security.PlatformPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 平台认证服务单测（无 DB，mock mapper）。 */
@ExtendWith(MockitoExtension.class)
class PlatformAuthServiceTest {

    @Mock
    private SysPlatformAdminMapper adminMapper;
    @Mock
    private SysPlatformAdminRoleMapper adminRoleMapper;
    @Mock
    private SysPlatformRolePermMapper rolePermMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PlatformAuthService authService;

    private SysPlatformAdmin admin(String status) {
        SysPlatformAdmin a = new SysPlatformAdmin();
        a.setId(1L);
        a.setUsername("superadmin");
        a.setPassword("hash");
        a.setName("超管");
        a.setStatus(status);
        return a;
    }

    @Test
    void loginWrongPasswordRejected() {
        when(adminMapper.selectOne(any())).thenReturn(admin("active"));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);
        assertThrows(BizException.class, () -> authService.login("superadmin", "bad"));
    }

    @Test
    void loginDisabledRejected() {
        when(adminMapper.selectOne(any())).thenReturn(admin("disabled"));
        when(passwordEncoder.matches("ok", "hash")).thenReturn(true);
        assertThrows(BizException.class, () -> authService.login("superadmin", "ok"));
    }

    @Test
    void loginSuccessReturnsIdAndStampsLastLogin() {
        when(adminMapper.selectOne(any())).thenReturn(admin("active"));
        when(passwordEncoder.matches("ok", "hash")).thenReturn(true);

        Long id = authService.login("superadmin", "ok");

        assertEquals(1L, id);
        verify(adminMapper).updateById(any(SysPlatformAdmin.class));
    }

    @Test
    void loadPrincipalCollectsPermCodes() {
        when(adminMapper.selectById(1L)).thenReturn(admin("active"));
        SysPlatformAdminRole ar = new SysPlatformAdminRole();
        ar.setAdminId(1L);
        ar.setRoleId(9L);
        when(adminRoleMapper.selectList(any())).thenReturn(List.of(ar));
        SysPlatformRolePerm p = new SysPlatformRolePerm();
        p.setRoleId(9L);
        p.setPermCode("platform:tenant:manage");
        when(rolePermMapper.selectList(any())).thenReturn(List.of(p));

        Optional<PlatformPrincipal> principal = authService.loadPrincipal(1L);

        assertTrue(principal.isPresent());
        assertTrue(principal.get().permCodes().contains("platform:tenant:manage"));
    }

    @Test
    void loadPrincipalOfDisabledIsEmpty() {
        when(adminMapper.selectById(1L)).thenReturn(admin("disabled"));
        assertTrue(authService.loadPrincipal(1L).isEmpty());
    }
}
