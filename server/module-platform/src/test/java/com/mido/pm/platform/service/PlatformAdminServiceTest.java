package com.mido.pm.platform.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.platform.dto.AdminCreateDTO;
import com.mido.pm.platform.dto.AdminUpdateDTO;
import com.mido.pm.platform.entity.SysPlatformAdmin;
import com.mido.pm.platform.entity.SysPlatformAdminRole;
import com.mido.pm.platform.entity.SysPlatformRole;
import com.mido.pm.platform.mapper.SysPlatformAdminMapper;
import com.mido.pm.platform.mapper.SysPlatformAdminRoleMapper;
import com.mido.pm.platform.mapper.SysPlatformRoleMapper;
import com.mido.pm.platform.security.PlatformContext;
import com.mido.pm.platform.security.PlatformPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 平台账号护栏单测：防自我提权、防自锁、保留末位超管（P0-a）。
 * 说明：super_admin 角色 id=1，mock 的 admin-role 关系统一指向该角色，
 * 故工具方法对“是否超管”的判定在不同账号上一致，用于聚焦护栏触发条件。
 */
@ExtendWith(MockitoExtension.class)
class PlatformAdminServiceTest {

    @Mock
    private SysPlatformAdminMapper adminMapper;
    @Mock
    private SysPlatformAdminRoleMapper adminRoleMapper;
    @Mock
    private SysPlatformRoleMapper roleMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PlatformAuditService auditService;

    @InjectMocks
    private PlatformAdminService service;

    @AfterEach
    void tearDown() {
        PlatformContext.clear();
    }

    private void login(Long adminId) {
        PlatformContext.set(new PlatformPrincipal(adminId, "u", "n", List.of(), false));
    }

    private SysPlatformRole superRole() {
        SysPlatformRole r = new SysPlatformRole();
        r.setId(1L);
        r.setCode("super_admin");
        return r;
    }

    private SysPlatformAdminRole adminRole(long adminId, long roleId) {
        SysPlatformAdminRole ar = new SysPlatformAdminRole();
        ar.setAdminId(adminId);
        ar.setRoleId(roleId);
        return ar;
    }

    private SysPlatformAdmin admin(long id) {
        SysPlatformAdmin a = new SysPlatformAdmin();
        a.setId(id);
        a.setStatus("active");
        return a;
    }

    @Test
    void nonSuperCannotGrantSuperRoleOnCreate() {
        login(100L); // 当前账号无任何角色 → 非超管
        when(adminMapper.selectCount(any())).thenReturn(0L);
        when(roleMapper.selectList(any())).thenReturn(List.of(superRole()));
        when(adminRoleMapper.selectList(any())).thenReturn(List.of()); // 当前账号无角色

        AdminCreateDTO dto = new AdminCreateDTO("newop", "新人", "Passw0rd", List.of(1L));
        assertThrows(BizException.class, () -> service.create(dto));
    }

    @Test
    void cannotDisableSelf() {
        login(100L);
        when(adminMapper.selectById(100L)).thenReturn(admin(100L));
        when(roleMapper.selectList(any())).thenReturn(List.of(superRole()));
        when(adminRoleMapper.selectList(any())).thenReturn(List.of()); // 目标=自己，非超管

        AdminUpdateDTO dto = new AdminUpdateDTO("我", "disabled", List.of(2L));
        assertThrows(BizException.class, () -> service.update(100L, dto));
    }

    @Test
    void cannotDisableLastActiveSuperAdmin() {
        login(2L); // 另一名超管操作
        when(adminMapper.selectById(1L)).thenReturn(admin(1L));
        when(roleMapper.selectList(any())).thenReturn(List.of(superRole()));
        when(adminRoleMapper.selectList(any())).thenReturn(List.of(adminRole(1L, 1L)));
        lenient().when(adminMapper.selectCount(any())).thenReturn(1L); // 仅剩 1 名启用超管

        AdminUpdateDTO dto = new AdminUpdateDTO("超管", "disabled", List.of(1L));
        assertThrows(BizException.class, () -> service.update(1L, dto));
    }
}
