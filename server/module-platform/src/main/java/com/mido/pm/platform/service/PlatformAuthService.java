package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.platform.entity.SysPlatformAdmin;
import com.mido.pm.platform.entity.SysPlatformAdminRole;
import com.mido.pm.platform.entity.SysPlatformRolePerm;
import com.mido.pm.platform.mapper.SysPlatformAdminMapper;
import com.mido.pm.platform.mapper.SysPlatformAdminRoleMapper;
import com.mido.pm.platform.mapper.SysPlatformRolePermMapper;
import com.mido.pm.platform.security.PlatformPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 平台认证服务：账号密码登录换取独立 JWT；按账号装配权限主体（供认证过滤器与 /me）。
 * P0-a：登录失败锁定（防暴破）、首登强制改密、自助改密。
 */
@Service
public class PlatformAuthService {

    private static final String STATUS_DISABLED = "disabled";
    /** 连续失败达到该次数即锁定。 */
    private static final int MAX_FAIL_BEFORE_LOCK = 5;
    /** 锁定时长（分钟）。 */
    private static final int LOCK_MINUTES = 15;

    private final SysPlatformAdminMapper adminMapper;
    private final SysPlatformAdminRoleMapper adminRoleMapper;
    private final SysPlatformRolePermMapper rolePermMapper;
    private final PasswordEncoder passwordEncoder;
    private final PlatformAuditService auditService;

    public PlatformAuthService(SysPlatformAdminMapper adminMapper,
                               SysPlatformAdminRoleMapper adminRoleMapper,
                               SysPlatformRolePermMapper rolePermMapper,
                               PasswordEncoder passwordEncoder,
                               PlatformAuditService auditService) {
        this.adminMapper = adminMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.rolePermMapper = rolePermMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /**
     * 校验账号密码。成功返回账号实体（含首登改密标记），令牌由控制器层签发。
     * 失败累计达阈值即锁定一段时间；锁定期内即便密码正确也拒绝。
     */
    public SysPlatformAdmin login(String username, String rawPassword) {
        SysPlatformAdmin admin = adminMapper.selectOne(
                Wrappers.<SysPlatformAdmin>lambdaQuery().eq(SysPlatformAdmin::getUsername, username));
        LocalDateTime now = LocalDateTime.now();
        if (admin != null && admin.getLockedUntil() != null && admin.getLockedUntil().isAfter(now)) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已锁定，请稍后再试");
        }
        if (admin == null || !passwordEncoder.matches(rawPassword, admin.getPassword())) {
            if (admin != null) {
                registerLoginFailure(admin, now);
            }
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        if (STATUS_DISABLED.equalsIgnoreCase(admin.getStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已停用");
        }
        // 成功：清零失败计数与锁定、记录登录时间（lockedUntil 字段标注 ALWAYS 策略，null 会被写入）
        admin.setFailCount(0);
        admin.setLockedUntil(null);
        admin.setLastLoginAt(now);
        adminMapper.updateById(admin);
        auditService.record(admin.getId(), PlatformAuditActions.ADMIN_LOGIN,
                PlatformAuditActions.TARGET_ADMIN, admin.getId(), java.util.Map.of("username", username));
        return admin;
    }

    private void registerLoginFailure(SysPlatformAdmin admin, LocalDateTime now) {
        int fails = (admin.getFailCount() == null ? 0 : admin.getFailCount()) + 1;
        if (fails >= MAX_FAIL_BEFORE_LOCK) {
            admin.setFailCount(0);
            admin.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
            adminMapper.updateById(admin);
            auditService.record(admin.getId(), PlatformAuditActions.ADMIN_LOGIN_LOCKED,
                    PlatformAuditActions.TARGET_ADMIN, admin.getId(),
                    java.util.Map.of("username", admin.getUsername(), "lockMinutes", LOCK_MINUTES));
        } else {
            admin.setFailCount(fails);
            adminMapper.updateById(admin);
        }
    }

    /** 当前账号自助改密（含首登强制改密）：校验原密码、写新密码并清除强制改密标记。 */
    @Transactional(rollbackFor = Exception.class)
    public void changeOwnPassword(Long adminId, String oldPassword, String newPassword) {
        if (adminId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未认证或登录已过期");
        }
        SysPlatformAdmin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "原密码错误");
        }
        admin.setPassword(passwordEncoder.encode(newPassword));
        admin.setMustChangePassword(false);
        adminMapper.updateById(admin);
        auditService.record(PlatformAuditActions.ADMIN_PASSWORD_CHANGED,
                PlatformAuditActions.TARGET_ADMIN, adminId, null);
    }

    /** 按账号 ID 装配权限主体；账号不存在或已停用返回空（供过滤器拒绝）。 */
    public Optional<PlatformPrincipal> loadPrincipal(Long adminId) {
        if (adminId == null) {
            return Optional.empty();
        }
        SysPlatformAdmin admin = adminMapper.selectById(adminId);
        if (admin == null || STATUS_DISABLED.equalsIgnoreCase(admin.getStatus())) {
            return Optional.empty();
        }
        return Optional.of(new PlatformPrincipal(
                admin.getId(), admin.getUsername(), admin.getName(), permCodesOf(adminId),
                Boolean.TRUE.equals(admin.getMustChangePassword())));
    }

    private List<String> permCodesOf(Long adminId) {
        List<Long> roleIds = adminRoleMapper.selectList(Wrappers.<SysPlatformAdminRole>lambdaQuery()
                        .eq(SysPlatformAdminRole::getAdminId, adminId))
                .stream().map(SysPlatformAdminRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return rolePermMapper.selectList(Wrappers.<SysPlatformRolePerm>lambdaQuery()
                        .in(SysPlatformRolePerm::getRoleId, roleIds))
                .stream().map(SysPlatformRolePerm::getPermCode).distinct().toList();
    }
}
