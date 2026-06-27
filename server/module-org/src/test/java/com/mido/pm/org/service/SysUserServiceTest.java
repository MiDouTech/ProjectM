package com.mido.pm.org.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.quota.QuotaGuard;
import com.mido.pm.org.dto.UserCreateDTO;
import com.mido.pm.org.entity.SysUser;
import com.mido.pm.org.mapper.SysUserMapper;
import com.mido.pm.org.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户服务单测：手机号作登录账号——查重冲突、用户名缺省取手机号、密码加密。
 */
@ExtendWith(MockitoExtension.class)
class SysUserServiceTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private QuotaGuard quotaGuard;
    @Mock
    private AuditLogService auditLogService;

    private SysUserService service() {
        return new SysUserService(userMapper, userRoleMapper, passwordEncoder, quotaGuard, auditLogService);
    }

    @Test
    void create_blankUsernameDefaultsToPhone_andEncodesPassword() {
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        lenient().when(passwordEncoder.encode("pwd")).thenReturn("ENC");

        service().create(new UserCreateDTO("13800138000", "  ", "张三", "pwd", 1L, "L1", null, "active"));

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(captor.capture());
        SysUser saved = captor.getValue();
        assertEquals("13800138000", saved.getPhone());
        assertEquals("13800138000", saved.getUsername(), "用户名缺省应取手机号");
        assertEquals("ENC", saved.getPassword(), "密码必须加密存储");
    }

    @Test
    void create_duplicatePhoneConflicts() {
        // 第一次查重（手机号）即命中
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BizException.class, () -> service()
                .create(new UserCreateDTO("13800138000", "zhangsan", "张三", "pwd", 1L, "L1", null, "active")));
    }

    private SysUser userWithPassword(String hash) {
        SysUser u = new SysUser();
        u.setId(7L);
        u.setPassword(hash);
        return u;
    }

    @Test
    void changePassword_wrongOldRejected() {
        when(userMapper.selectById(7L)).thenReturn(userWithPassword("OLD_ENC"));
        when(passwordEncoder.matches("wrong", "OLD_ENC")).thenReturn(false);

        assertThrows(BizException.class, () -> service().changePassword(7L, "wrong", "newPass123"));
        verify(userMapper, org.mockito.Mockito.never()).updateById(any(SysUser.class));
    }

    @Test
    void changePassword_sameAsOldRejected() {
        when(userMapper.selectById(7L)).thenReturn(userWithPassword("OLD_ENC"));
        // 原密码校验通过；新密码与原密码相同（同一明文 → 同一 stub 命中两次）应被拒
        when(passwordEncoder.matches("oldPass123", "OLD_ENC")).thenReturn(true);

        assertThrows(BizException.class, () -> service().changePassword(7L, "oldPass123", "oldPass123"));
        verify(userMapper, org.mockito.Mockito.never()).updateById(any(SysUser.class));
    }

    @Test
    void changePassword_success_encodesAndAudits() {
        when(userMapper.selectById(7L)).thenReturn(userWithPassword("OLD_ENC"));
        when(passwordEncoder.matches("oldPass123", "OLD_ENC")).thenReturn(true);
        when(passwordEncoder.matches("newPass123", "OLD_ENC")).thenReturn(false);
        when(passwordEncoder.encode("newPass123")).thenReturn("NEW_ENC");

        service().changePassword(7L, "oldPass123", "newPass123");

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals("NEW_ENC", captor.getValue().getPassword(), "新密码必须加密落库");
        verify(auditLogService).record(any(), any(), any(), any(), any());
    }

    @Test
    void resetPassword_success_encodesAndAudits() {
        when(userMapper.selectById(7L)).thenReturn(userWithPassword("OLD_ENC"));
        when(passwordEncoder.encode("resetPass123")).thenReturn("RESET_ENC");

        service().resetPassword(7L, "resetPass123");

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals("RESET_ENC", captor.getValue().getPassword(), "重置密码必须加密落库");
        verify(auditLogService).record(any(), any(), any(), any(), any());
    }
}
