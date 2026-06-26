package com.mido.pm.project.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.project.dto.ProjectRoleSaveDTO;
import com.mido.pm.project.entity.PmProjectRole;
import com.mido.pm.project.mapper.PmProjectRoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 项目角色服务单测：编码查重、内置不可删。
 */
@ExtendWith(MockitoExtension.class)
class ProjectRoleServiceTest {

    @Mock private PmProjectRoleMapper roleMapper;

    private ProjectRoleService service() {
        return new ProjectRoleService(roleMapper);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(roleMapper.selectCount(any())).thenReturn(1L);
        assertThrows(BizException.class, () -> service().create(new ProjectRoleSaveDTO("开发", "开发", 1, "active")));
        verify(roleMapper, never()).insert(any(PmProjectRole.class));
    }

    @Test
    void deleteBuiltinRoleRejected() {
        PmProjectRole builtin = new PmProjectRole();
        builtin.setId(1L);
        builtin.setBuiltin(1);
        when(roleMapper.selectById(1L)).thenReturn(builtin);
        assertThrows(BizException.class, () -> service().delete(1L));
        verify(roleMapper, never()).deleteById(any(java.io.Serializable.class));
    }

    @Test
    void createPersistsCustomRole() {
        when(roleMapper.selectCount(any())).thenReturn(0L);
        service().create(new ProjectRoleSaveDTO("开发", "开发工程师", 5, null));
        verify(roleMapper).insert(any(PmProjectRole.class));
    }
}
