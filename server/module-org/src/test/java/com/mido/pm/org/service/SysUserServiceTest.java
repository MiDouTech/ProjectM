package com.mido.pm.org.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    private SysUserService service() {
        return new SysUserService(userMapper, userRoleMapper, passwordEncoder, quotaGuard);
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
}
