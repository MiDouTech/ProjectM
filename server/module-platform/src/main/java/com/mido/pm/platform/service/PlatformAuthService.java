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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 平台认证服务：账号密码登录换取独立 JWT；按账号装配权限主体（供认证过滤器与 /me）。
 */
@Service
public class PlatformAuthService {

    private static final String STATUS_DISABLED = "disabled";

    private final SysPlatformAdminMapper adminMapper;
    private final SysPlatformAdminRoleMapper adminRoleMapper;
    private final SysPlatformRolePermMapper rolePermMapper;
    private final PasswordEncoder passwordEncoder;

    public PlatformAuthService(SysPlatformAdminMapper adminMapper,
                               SysPlatformAdminRoleMapper adminRoleMapper,
                               SysPlatformRolePermMapper rolePermMapper,
                               PasswordEncoder passwordEncoder) {
        this.adminMapper = adminMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.rolePermMapper = rolePermMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 校验账号密码，成功返回平台账号 ID（令牌由控制器层签发）。 */
    public Long login(String username, String rawPassword) {
        SysPlatformAdmin admin = adminMapper.selectOne(
                Wrappers.<SysPlatformAdmin>lambdaQuery().eq(SysPlatformAdmin::getUsername, username));
        if (admin == null || !passwordEncoder.matches(rawPassword, admin.getPassword())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        if (STATUS_DISABLED.equalsIgnoreCase(admin.getStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已停用");
        }
        admin.setLastLoginAt(LocalDateTime.now());
        adminMapper.updateById(admin);
        return admin.getId();
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
                admin.getId(), admin.getUsername(), admin.getName(), permCodesOf(adminId)));
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
