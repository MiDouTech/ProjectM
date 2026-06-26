package com.mido.pm.org.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.audit.AuditActions;
import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.common.audit.Audited;
import com.mido.pm.common.datascope.DataScopeContext;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.quota.QuotaGuard;
import com.mido.pm.common.quota.QuotaResources;
import com.mido.pm.org.dto.UserCreateDTO;
import com.mido.pm.org.dto.UserQueryDTO;
import com.mido.pm.org.dto.UserUpdateDTO;
import com.mido.pm.org.dto.UserVO;
import com.mido.pm.org.entity.SysUser;
import com.mido.pm.org.entity.SysUserRole;
import com.mido.pm.org.mapper.SysUserMapper;
import com.mido.pm.org.mapper.SysUserRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 用户服务：CRUD（含 job_level）+ 分配角色。
 * 列表查询挂接数据范围（resource=user），演示可复用拦截器自动注入。
 */
@Service
public class SysUserService {

    /** 用户资源数据范围标识 */
    public static final String RESOURCE = "user";

    private static final long MAX_PAGE_SIZE = 100L;

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final QuotaGuard quotaGuard;
    private final AuditLogService auditLogService;

    public SysUserService(SysUserMapper userMapper, SysUserRoleMapper userRoleMapper,
                          PasswordEncoder passwordEncoder, QuotaGuard quotaGuard,
                          AuditLogService auditLogService) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.quotaGuard = quotaGuard;
        this.auditLogService = auditLogService;
    }

    public PageResult<UserVO> page(UserQueryDTO query) {
        long pageNo = query.page() == null || query.page() < 1 ? 1 : query.page();
        long size = query.size() == null || query.size() < 1 ? 20 : Math.min(query.size(), MAX_PAGE_SIZE);

        Page<SysUser> page = new Page<>(pageNo, size);
        var wrapper = Wrappers.<SysUser>lambdaQuery()
                // 选人/搜索关键字同时匹配账号与姓名（中文姓名存于 name），提升选人体验
                .and(StrUtil.isNotBlank(query.username()),
                        w -> w.like(SysUser::getUsername, query.username())
                                .or().like(SysUser::getName, query.username()))
                .eq(query.deptId() != null, SysUser::getDeptId, query.deptId())
                .eq(StrUtil.isNotBlank(query.status()), SysUser::getStatus, query.status())
                .orderByDesc(SysUser::getId);

        // 声明数据范围：列表按当前用户角色的 user 资源范围注入 dept_id/create_by 条件。
        // scoped 封装 set+clear，模块无需各自写 try/finally。
        Page<SysUser> result = DataScopeContext.scoped(RESOURCE, "dept_id", "create_by",
                () -> userMapper.selectPage(page, wrapper));

        List<UserVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotal(), pageNo, size);
    }

    public UserVO get(Long id) {
        return toVO(requireExists(id));
    }

    @Audited(module = AuditActions.MODULE_MEMBER, action = AuditActions.CREATED,
            target = AuditActions.TARGET_USER)
    public Long create(UserCreateDTO dto) {
        // 配额硬校验：当前租户成员数不得超过订阅套餐上限（不限/未订阅则放行）
        Long currentUserCount = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery());
        quotaGuard.checkCanAdd(QuotaResources.USER, currentUserCount == null ? 0L : currentUserCount);

        Long phoneExists = userMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getPhone, dto.phone()));
        if (phoneExists != null && phoneExists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "手机号已注册");
        }
        // 用户名可选，缺省取手机号；仍校验用户名唯一（双登录两条标识都不可重复）
        String username = StrUtil.isBlank(dto.username()) ? dto.phone() : dto.username();
        Long nameExists = userMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (nameExists != null && nameExists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setPhone(dto.phone());
        user.setUsername(username);
        user.setName(dto.name());
        user.setAvatar(dto.avatar());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setDeptId(dto.deptId());
        user.setJobLevel(dto.jobLevel());
        user.setStatus(dto.status());
        userMapper.insert(user);
        return user.getId();
    }

    @Audited(module = AuditActions.MODULE_MEMBER, action = AuditActions.UPDATED,
            target = AuditActions.TARGET_USER)
    public void update(Long id, UserUpdateDTO dto) {
        SysUser user = requireExists(id);
        user.setName(dto.name());
        user.setAvatar(dto.avatar());
        user.setDeptId(dto.deptId());
        user.setJobLevel(dto.jobLevel());
        user.setStatus(dto.status());
        userMapper.updateById(user);
    }

    @Audited(module = AuditActions.MODULE_MEMBER, action = AuditActions.DELETED,
            target = AuditActions.TARGET_USER)
    public void delete(Long id) {
        requireExists(id);
        userMapper.deleteById(id);
    }

    /** 用户的角色 id 集合（供跨域权限解析，如文档 ACL）。 */
    public List<Long> roleIdsOf(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
    }

    /** 重设用户的角色集合（先清后插）。 */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        requireExists(userId);
        List<Long> before = roleIdsOf(userId);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
        // 账号权限变更：同事务记录角色 before/after（雪花 ID 经全局序列化为字符串）
        auditLogService.record(AuditActions.MODULE_PERMISSION, AuditActions.TARGET_USER, userId,
                AuditActions.ROLES_ASSIGNED,
                Map.of("from", before, "to", roleIds == null ? List.of() : roleIds));
    }

    private SysUser requireExists(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private UserVO toVO(SysUser u) {
        return new UserVO(u.getId(), u.getPhone(), u.getUsername(), u.getName(), u.getAvatar(),
                u.getDeptId(), u.getJobLevel(), u.getStatus(), u.getCreateTime());
    }
}
