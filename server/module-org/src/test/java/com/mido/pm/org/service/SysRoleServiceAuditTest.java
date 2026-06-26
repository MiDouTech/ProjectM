package com.mido.pm.org.service;

import com.mido.pm.common.audit.AuditActions;
import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.common.security.FieldAccess;
import com.mido.pm.org.dto.FieldPermSettingDTO;
import com.mido.pm.org.entity.SysRole;
import com.mido.pm.org.entity.SysRolePerm;
import com.mido.pm.org.mapper.SysFieldPermMapper;
import com.mido.pm.org.mapper.SysRoleCustomDeptMapper;
import com.mido.pm.org.mapper.SysRoleDataScopeMapper;
import com.mido.pm.org.mapper.SysRoleMapper;
import com.mido.pm.org.mapper.SysRolePermMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 角色服务审计单测：权限码 / 数据范围变更必须同事务写一条 before/after 审计。
 */
@ExtendWith(MockitoExtension.class)
class SysRoleServiceAuditTest {

    @Mock private SysRoleMapper roleMapper;
    @Mock private SysRolePermMapper rolePermMapper;
    @Mock private SysRoleDataScopeMapper roleDataScopeMapper;
    @Mock private SysFieldPermMapper fieldPermMapper;
    @Mock private SysRoleCustomDeptMapper roleCustomDeptMapper;
    @Mock private AuditLogService auditLogService;

    private SysRoleService service() {
        return new SysRoleService(roleMapper, rolePermMapper, roleDataScopeMapper,
                fieldPermMapper, roleCustomDeptMapper, auditLogService);
    }

    @Test
    void savePermsRecordsPermsChangedAudit() {
        SysRole role = new SysRole();
        role.setId(1L);
        when(roleMapper.selectById(1L)).thenReturn(role);
        SysRolePerm existing = new SysRolePerm();
        existing.setRoleId(1L);
        existing.setPermCode("org:user:query");
        when(rolePermMapper.selectList(any())).thenReturn(List.of(existing));

        service().savePerms(1L, List.of("org:user:query", "org:role:create"));

        verify(rolePermMapper).delete(any());
        verify(rolePermMapper, times(2)).insert(any(SysRolePerm.class));
        verify(auditLogService).record(eq(AuditActions.MODULE_PERMISSION), eq(AuditActions.TARGET_ROLE),
                eq(1L), eq(AuditActions.PERMS_CHANGED), any());
    }

    @Test
    void saveDataScopesRecordsDataScopeChangedAudit() {
        SysRole role = new SysRole();
        role.setId(2L);
        when(roleMapper.selectById(2L)).thenReturn(role);
        when(roleDataScopeMapper.selectList(any())).thenReturn(List.of());

        service().saveDataScopes(2L, List.of());

        verify(auditLogService).record(eq(AuditActions.MODULE_PERMISSION), eq(AuditActions.TARGET_ROLE),
                eq(2L), eq(AuditActions.DATA_SCOPE_CHANGED), any());
    }

    @Test
    void saveFieldPermsRecordsFieldPermChangedAudit() {
        SysRole role = new SysRole();
        role.setId(3L);
        when(roleMapper.selectById(3L)).thenReturn(role);
        when(fieldPermMapper.selectList(any())).thenReturn(List.of());

        service().saveFieldPerms(3L, List.of(new FieldPermSettingDTO("task", "priority", FieldAccess.VIEW)));

        verify(fieldPermMapper).insert(any(com.mido.pm.org.entity.SysFieldPerm.class));
        verify(auditLogService).record(eq(AuditActions.MODULE_PERMISSION), eq(AuditActions.TARGET_ROLE),
                eq(3L), eq(AuditActions.FIELD_PERM_CHANGED), any());
    }
}
