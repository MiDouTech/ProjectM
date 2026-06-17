package com.mido.pm.org.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mido.pm.common.api.PageResult;
import com.mido.pm.common.datascope.DataScopeContext;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
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

    public SysUserService(SysUserMapper userMapper, SysUserRoleMapper userRoleMapper,
                          PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResult<UserVO> page(UserQueryDTO query) {
        long pageNo = query.page() == null || query.page() < 1 ? 1 : query.page();
        long size = query.size() == null || query.size() < 1 ? 20 : Math.min(query.size(), MAX_PAGE_SIZE);

        Page<SysUser> page = new Page<>(pageNo, size);
        var wrapper = Wrappers.<SysUser>lambdaQuery()
                .like(StrUtil.isNotBlank(query.username()), SysUser::getUsername, query.username())
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

    public Long create(UserCreateDTO dto) {
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
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setDeptId(dto.deptId());
        user.setJobLevel(dto.jobLevel());
        user.setStatus(dto.status());
        userMapper.insert(user);
        return user.getId();
    }

    public void update(Long id, UserUpdateDTO dto) {
        SysUser user = requireExists(id);
        user.setName(dto.name());
        user.setDeptId(dto.deptId());
        user.setJobLevel(dto.jobLevel());
        user.setStatus(dto.status());
        userMapper.updateById(user);
    }

    public void delete(Long id) {
        requireExists(id);
        userMapper.deleteById(id);
    }

    /** 重设用户的角色集合（先清后插）。 */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        requireExists(userId);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }

    private SysUser requireExists(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private UserVO toVO(SysUser u) {
        return new UserVO(u.getId(), u.getPhone(), u.getUsername(), u.getName(), u.getDeptId(),
                u.getJobLevel(), u.getStatus(), u.getCreateTime());
    }
}
