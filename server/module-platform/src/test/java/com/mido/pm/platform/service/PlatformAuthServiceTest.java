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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    @Mock
    private PlatformAuditService auditService;

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
    void loginWrongPasswordRecordsFailure() {
        when(adminMapper.selectOne(any())).thenReturn(admin("active"));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);
        assertThrows(BizException.class, () -> authService.login("superadmin", "bad"));
        // 失败计数被写回
        verify(adminMapper).updateById(any(SysPlatformAdmin.class));
    }

    @Test
    void loginDisabledRejected() {
        when(adminMapper.selectOne(any())).thenReturn(admin("disabled"));
        when(passwordEncoder.matches("ok", "hash")).thenReturn(true);
        assertThrows(BizException.class, () -> authService.login("superadmin", "ok"));
    }

    @Test
    void loginSuccessReturnsAdminAndClearsLock() {
        when(adminMapper.selectOne(any())).thenReturn(admin("active"));
        when(passwordEncoder.matches("ok", "hash")).thenReturn(true);

        SysPlatformAdmin result = authService.login("superadmin", "ok");

        assertEquals(1L, result.getId());
        // 成功路径清零锁定与失败计数后写回
        verify(adminMapper).updateById(any(SysPlatformAdmin.class));
    }

    @Test
    void lockedAccountRejectedEvenWithCorrectPassword() {
        SysPlatformAdmin locked = admin("active");
        locked.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(adminMapper.selectOne(any())).thenReturn(locked);

        assertThrows(BizException.class, () -> authService.login("superadmin", "ok"));
        // 锁定期内不校验密码、不写库
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void fifthFailureTriggersLock() {
        SysPlatformAdmin a = admin("active");
        a.setFailCount(4); // 第 5 次失败触发锁定
        when(adminMapper.selectOne(any())).thenReturn(a);
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThrows(BizException.class, () -> authService.login("superadmin", "bad"));

        assertTrue(a.getLockedUntil() != null && a.getLockedUntil().isAfter(LocalDateTime.now()));
        verify(adminMapper).updateById(any(SysPlatformAdmin.class));
    }

    @Test
    void changeOwnPasswordWrongOldRejected() {
        when(adminMapper.selectById(1L)).thenReturn(admin("active"));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        assertThrows(BizException.class, () -> authService.changeOwnPassword(1L, "wrong", "NewPass123"));
    }

    @Test
    void changeOwnPasswordSuccessClearsFlag() {
        SysPlatformAdmin a = admin("active");
        a.setMustChangePassword(true);
        when(adminMapper.selectById(1L)).thenReturn(a);
        when(passwordEncoder.matches("OldPass123", "hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPass123")).thenReturn("newhash");

        authService.changeOwnPassword(1L, "OldPass123", "NewPass123");

        assertEquals("newhash", a.getPassword());
        assertFalse(a.getMustChangePassword());
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
