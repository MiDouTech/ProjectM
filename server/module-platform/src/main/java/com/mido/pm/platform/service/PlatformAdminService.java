package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.platform.dto.AdminCreateDTO;
import com.mido.pm.platform.dto.AdminUpdateDTO;
import com.mido.pm.platform.dto.AdminVO;
import com.mido.pm.platform.dto.PlatformRoleVO;
import com.mido.pm.platform.dto.ResetPasswordDTO;
import com.mido.pm.platform.entity.SysPlatformAdmin;
import com.mido.pm.platform.entity.SysPlatformAdminRole;
import com.mido.pm.platform.entity.SysPlatformRole;
import com.mido.pm.platform.mapper.SysPlatformAdminMapper;
import com.mido.pm.platform.mapper.SysPlatformAdminRoleMapper;
import com.mido.pm.platform.mapper.SysPlatformRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 平台账号与角色管理：账号 CRUD、分配角色、重置密码。角色为内置只读集合（不在 P0 开放增删）。
 */
@Service
public class PlatformAdminService {

    private final SysPlatformAdminMapper adminMapper;
    private final SysPlatformAdminRoleMapper adminRoleMapper;
    private final SysPlatformRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final PlatformAuditService auditService;

    public PlatformAdminService(SysPlatformAdminMapper adminMapper,
                                SysPlatformAdminRoleMapper adminRoleMapper,
                                SysPlatformRoleMapper roleMapper, PasswordEncoder passwordEncoder,
                                PlatformAuditService auditService) {
        this.adminMapper = adminMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public List<PlatformRoleVO> listRoles() {
        return roleMapper.selectList(Wrappers.<SysPlatformRole>lambdaQuery().orderByAsc(SysPlatformRole::getId))
                .stream().map(r -> new PlatformRoleVO(r.getId(), r.getName(), r.getCode(), r.getRemark())).toList();
    }

    public List<AdminVO> list() {
        List<SysPlatformAdmin> admins = adminMapper.selectList(
                Wrappers.<SysPlatformAdmin>lambdaQuery().orderByDesc(SysPlatformAdmin::getId));
        Map<Long, String> roleNames = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysPlatformRole::getId, SysPlatformRole::getName));
        return admins.stream().map(a -> toVO(a, roleNames)).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(AdminCreateDTO dto) {
        Long dup = adminMapper.selectCount(
                Wrappers.<SysPlatformAdmin>lambdaQuery().eq(SysPlatformAdmin::getUsername, dto.username()));
        if (dup != null && dup > 0) {
            throw new BizException(ErrorCode.CONFLICT, "登录名已存在");
        }
        SysPlatformAdmin admin = new SysPlatformAdmin();
        admin.setUsername(dto.username());
        admin.setName(dto.name());
        admin.setPassword(passwordEncoder.encode(dto.password()));
        admin.setStatus("active");
        adminMapper.insert(admin);
        replaceRoles(admin.getId(), dto.roleIds());
        auditService.record(PlatformAuditActions.ADMIN_CREATED, PlatformAuditActions.TARGET_ADMIN, admin.getId(),
                Map.of("username", dto.username(), "name", dto.name()));
        return admin.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AdminUpdateDTO dto) {
        SysPlatformAdmin admin = requireExists(id);
        admin.setName(dto.name());
        admin.setStatus(dto.status());
        adminMapper.updateById(admin);
        replaceRoles(id, dto.roleIds());
        auditService.record(PlatformAuditActions.ADMIN_UPDATED, PlatformAuditActions.TARGET_ADMIN, id,
                Map.of("name", dto.name(), "status", dto.status()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, ResetPasswordDTO dto) {
        SysPlatformAdmin admin = requireExists(id);
        admin.setPassword(passwordEncoder.encode(dto.password()));
        adminMapper.updateById(admin);
        auditService.record(PlatformAuditActions.ADMIN_PASSWORD_RESET, PlatformAuditActions.TARGET_ADMIN, id, null);
    }

    private void replaceRoles(Long adminId, List<Long> roleIds) {
        adminRoleMapper.delete(Wrappers.<SysPlatformAdminRole>lambdaQuery()
                .eq(SysPlatformAdminRole::getAdminId, adminId));
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            SysPlatformAdminRole ar = new SysPlatformAdminRole();
            ar.setAdminId(adminId);
            ar.setRoleId(roleId);
            adminRoleMapper.insert(ar);
        }
    }

    private List<Long> roleIdsOf(Long adminId) {
        return adminRoleMapper.selectList(Wrappers.<SysPlatformAdminRole>lambdaQuery()
                        .eq(SysPlatformAdminRole::getAdminId, adminId))
                .stream().map(SysPlatformAdminRole::getRoleId).toList();
    }

    private AdminVO toVO(SysPlatformAdmin a, Map<Long, String> roleNames) {
        List<Long> roleIds = roleIdsOf(a.getId());
        List<String> names = roleIds.stream().map(roleNames::get).filter(java.util.Objects::nonNull).toList();
        return new AdminVO(a.getId(), a.getUsername(), a.getName(), a.getStatus(),
                roleIds, names, a.getLastLoginAt(), a.getCreateTime());
    }

    private SysPlatformAdmin requireExists(Long id) {
        SysPlatformAdmin admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        return admin;
    }
}
