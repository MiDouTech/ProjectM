package com.mido.pm.org.service;

import com.mido.pm.common.audit.AuditActions;
import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.common.quota.QuotaGuard;
import com.mido.pm.org.entity.SysUser;
import com.mido.pm.org.entity.SysUserRole;
import com.mido.pm.org.mapper.SysUserMapper;
import com.mido.pm.org.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户服务审计单测：分配角色（账号权限变更）必须写一条 ROLES_ASSIGNED 审计。
 */
@ExtendWith(MockitoExtension.class)
class SysUserServiceAuditTest {

    @Mock private SysUserMapper userMapper;
    @Mock private SysUserRoleMapper userRoleMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private QuotaGuard quotaGuard;
    @Mock private AuditLogService auditLogService;

    private SysUserService service() {
        return new SysUserService(userMapper, userRoleMapper, passwordEncoder, quotaGuard, auditLogService);
    }

    @Test
    void assignRolesRecordsRolesAssignedAudit() {
        SysUser user = new SysUser();
        user.setId(7L);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(userRoleMapper.selectList(any())).thenReturn(List.of());

        service().assignRoles(7L, List.of(10L, 11L));

        verify(userRoleMapper).delete(any());
        verify(auditLogService).record(eq(AuditActions.MODULE_PERMISSION), eq(AuditActions.TARGET_USER),
                eq(7L), eq(AuditActions.ROLES_ASSIGNED), any());
    }
}
