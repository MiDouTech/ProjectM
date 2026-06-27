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
import com.mido.pm.platform.security.PlatformContext;
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

    private static final String SUPER_ADMIN_CODE = "super_admin";
    private static final String STATUS_DISABLED = "disabled";

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
        guardGrantSuper(dto.roleIds());
        SysPlatformAdmin admin = new SysPlatformAdmin();
        admin.setUsername(dto.username());
        admin.setName(dto.name());
        admin.setPassword(passwordEncoder.encode(dto.password()));
        admin.setStatus("active");
        // 新建账号初始密码由管理员设置，首次登录强制改密
        admin.setMustChangePassword(true);
        adminMapper.insert(admin);
        replaceRoles(admin.getId(), dto.roleIds());
        auditService.record(PlatformAuditActions.ADMIN_CREATED, PlatformAuditActions.TARGET_ADMIN, admin.getId(),
                Map.of("username", dto.username(), "name", dto.name()));
        return admin.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AdminUpdateDTO dto) {
        SysPlatformAdmin admin = requireExists(id);
        Long currentId = PlatformContext.currentAdminId();
        boolean targetWasSuper = isSuperAdmin(id);
        boolean willBeSuper = rolesContainSuper(dto.roleIds());
        boolean willDisable = STATUS_DISABLED.equalsIgnoreCase(dto.status());

        // 1) 非超管不得修改超管账号
        guardManageSuperTarget(currentId, targetWasSuper);
        // 2) 仅超管可授予超管角色（防自我提权）
        if (willBeSuper && !targetWasSuper) {
            guardGrantSuper(dto.roleIds());
        }
        // 3) 不得停用自己（防自锁）
        if (id.equals(currentId) && willDisable) {
            throw new BizException(ErrorCode.CONFLICT, "不能停用当前登录的账号");
        }
        // 4) 必须保留至少一名启用的超管
        if (targetWasSuper && (willDisable || !willBeSuper) && countActiveSuperAdmins() <= 1) {
            throw new BizException(ErrorCode.CONFLICT, "必须保留至少一名启用的超级管理员");
        }

        // 记录前值用于审计追溯
        String statusFrom = admin.getStatus();
        List<Long> rolesFrom = roleIdsOf(id);

        admin.setName(dto.name());
        admin.setStatus(dto.status());
        adminMapper.updateById(admin);
        replaceRoles(id, dto.roleIds());
        Map<String, Object> detail = new java.util.HashMap<>();
        detail.put("name", dto.name());
        detail.put("statusFrom", statusFrom);
        detail.put("statusTo", dto.status());
        detail.put("rolesFrom", rolesFrom);
        detail.put("rolesTo", dto.roleIds() == null ? List.of() : dto.roleIds());
        auditService.record(PlatformAuditActions.ADMIN_UPDATED, PlatformAuditActions.TARGET_ADMIN, id, detail);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, ResetPasswordDTO dto) {
        SysPlatformAdmin admin = requireExists(id);
        // 非超管不得重置超管账号口令
        guardManageSuperTarget(PlatformContext.currentAdminId(), isSuperAdmin(id));
        admin.setPassword(passwordEncoder.encode(dto.password()));
        // 被重置者下次登录强制改密
        admin.setMustChangePassword(true);
        adminMapper.updateById(admin);
        auditService.record(PlatformAuditActions.ADMIN_PASSWORD_RESET, PlatformAuditActions.TARGET_ADMIN, id, null);
    }

    /** 目标为超管时，操作者必须也是超管。 */
    private void guardManageSuperTarget(Long currentId, boolean targetIsSuper) {
        if (targetIsSuper && !isSuperAdmin(currentId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作超级管理员账号");
        }
    }

    /** 授予超管角色时，操作者必须是超管。 */
    private void guardGrantSuper(List<Long> roleIds) {
        if (rolesContainSuper(roleIds) && !isSuperAdmin(PlatformContext.currentAdminId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅超级管理员可授予超级管理员角色");
        }
    }

    private List<Long> superRoleIds() {
        return roleMapper.selectList(Wrappers.<SysPlatformRole>lambdaQuery()
                        .eq(SysPlatformRole::getCode, SUPER_ADMIN_CODE))
                .stream().map(SysPlatformRole::getId).toList();
    }

    private boolean rolesContainSuper(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return false;
        }
        return superRoleIds().stream().anyMatch(roleIds::contains);
    }

    private boolean isSuperAdmin(Long adminId) {
        if (adminId == null) {
            return false;
        }
        return rolesContainSuper(roleIdsOf(adminId));
    }

    private long countActiveSuperAdmins() {
        List<Long> superRoles = superRoleIds();
        if (superRoles.isEmpty()) {
            return 0;
        }
        List<Long> adminIds = adminRoleMapper.selectList(Wrappers.<SysPlatformAdminRole>lambdaQuery()
                        .in(SysPlatformAdminRole::getRoleId, superRoles))
                .stream().map(SysPlatformAdminRole::getAdminId).distinct().toList();
        if (adminIds.isEmpty()) {
            return 0;
        }
        Long cnt = adminMapper.selectCount(Wrappers.<SysPlatformAdmin>lambdaQuery()
                .in(SysPlatformAdmin::getId, adminIds)
                .ne(SysPlatformAdmin::getStatus, STATUS_DISABLED));
        return cnt == null ? 0 : cnt;
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
