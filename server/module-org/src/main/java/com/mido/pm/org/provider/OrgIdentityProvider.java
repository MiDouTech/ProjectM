package com.mido.pm.org.provider;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.security.DataScopeResolver;
import com.mido.pm.common.security.FieldAccess;
import com.mido.pm.org.entity.SysDept;
import com.mido.pm.org.entity.SysFieldPerm;
import com.mido.pm.org.entity.SysRoleCustomDept;
import com.mido.pm.org.entity.SysRoleDataScope;
import com.mido.pm.org.entity.SysRolePerm;
import com.mido.pm.org.entity.SysUser;
import com.mido.pm.org.entity.SysUserRole;
import com.mido.pm.org.mapper.SysDeptMapper;
import com.mido.pm.org.mapper.SysFieldPermMapper;
import com.mido.pm.org.mapper.SysRoleCustomDeptMapper;
import com.mido.pm.org.mapper.SysRoleDataScopeMapper;
import com.mido.pm.org.mapper.SysRolePermMapper;
import com.mido.pm.org.mapper.SysUserMapper;
import com.mido.pm.org.mapper.SysUserRoleMapper;
import com.mido.pm.provider.identity.IdentityProvider;
import com.mido.pm.provider.identity.UserPrincipal;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * IdentityProvider 的本地实现（读 sys_*）。
 * 因需访问业务表，置于 module-org 以避免 provider 依赖业务模块（分层不成环）。
 * 装配用户的权限码、各资源数据范围（多角色取最宽）、下属部门集。
 */
@Component
public class OrgIdentityProvider implements IdentityProvider {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermMapper rolePermMapper;
    private final SysRoleDataScopeMapper roleDataScopeMapper;
    private final SysFieldPermMapper fieldPermMapper;
    private final SysRoleCustomDeptMapper roleCustomDeptMapper;
    private final SysDeptMapper deptMapper;

    public OrgIdentityProvider(SysUserMapper userMapper, SysUserRoleMapper userRoleMapper,
                               SysRolePermMapper rolePermMapper, SysRoleDataScopeMapper roleDataScopeMapper,
                               SysFieldPermMapper fieldPermMapper, SysRoleCustomDeptMapper roleCustomDeptMapper,
                               SysDeptMapper deptMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermMapper = rolePermMapper;
        this.roleDataScopeMapper = roleDataScopeMapper;
        this.fieldPermMapper = fieldPermMapper;
        this.roleCustomDeptMapper = roleCustomDeptMapper;
        this.deptMapper = deptMapper;
    }

    @Override
    public Optional<UserPrincipal> loadByUsername(String username) {
        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        return user == null ? Optional.empty() : Optional.of(build(user));
    }

    @Override
    public Optional<UserPrincipal> loadByPhone(String phone) {
        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getPhone, phone));
        return user == null ? Optional.empty() : Optional.of(build(user));
    }

    @Override
    public Optional<UserPrincipal> loadById(Long userId) {
        SysUser user = userMapper.selectById(userId);
        return user == null ? Optional.empty() : Optional.of(build(user));
    }

    private UserPrincipal build(SysUser user) {
        UserPrincipal p = new UserPrincipal();
        p.setUserId(user.getId());
        p.setTenantId(user.getTenantId());
        p.setUsername(user.getUsername());
        p.setPhone(user.getPhone());
        p.setName(user.getName());
        p.setPasswordHash(user.getPassword());
        p.setDeptId(user.getDeptId());
        p.setJobLevel(user.getJobLevel());
        p.setStatus(user.getStatus());

        List<Long> roleIds = userRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, user.getId()))
                .stream().map(SysUserRole::getRoleId).toList();

        if (!roleIds.isEmpty()) {
            p.setPermCodes(rolePermMapper.selectList(
                            Wrappers.<SysRolePerm>lambdaQuery().in(SysRolePerm::getRoleId, roleIds))
                    .stream().map(SysRolePerm::getPermCode).distinct().toList());

            List<DataScopeResolver.RoleScope> rawScopes = roleDataScopeMapper.selectList(
                            Wrappers.<SysRoleDataScope>lambdaQuery().in(SysRoleDataScope::getRoleId, roleIds))
                    .stream().map(s -> new DataScopeResolver.RoleScope(s.getResource(), s.getScope())).toList();
            p.setResourceScopes(DataScopeResolver.mergeBroadest(rawScopes));

            p.setViewOnlyFields(computeViewOnlyFields(roleIds));

            p.setCustomDeptIds(roleCustomDeptMapper.selectList(
                            Wrappers.<SysRoleCustomDept>lambdaQuery().in(SysRoleCustomDept::getRoleId, roleIds))
                    .stream().map(SysRoleCustomDept::getDeptId).distinct().toList());
        }

        p.setSubDeptIds(subDeptIds(user.getDeptId()));
        return p;
    }

    private Set<String> computeViewOnlyFields(List<Long> roleIds) {
        List<SysFieldPerm> perms = fieldPermMapper.selectList(
                Wrappers.<SysFieldPerm>lambdaQuery().in(SysFieldPerm::getRoleId, roleIds));
        return mergeViewOnly(perms, roleIds.size());
    }

    /**
     * 多角色合并取最宽：与数据范围「未配置=最宽(ALL)」一致——未配置某字段的角色默认授予 edit。
     * 因此某字段仅当用户【所有】角色都显式将其设为 view 时才只读；
     * 任一角色给 edit、或任一角色未配置该字段，即视为可编辑。键形如 "resource.field"。
     */
    static Set<String> mergeViewOnly(List<SysFieldPerm> perms, int totalRoles) {
        Set<String> editKeys = new HashSet<>();
        Map<String, Set<Long>> viewRoles = new HashMap<>();
        for (SysFieldPerm fp : perms) {
            String key = fp.getResource() + "." + fp.getField();
            if (FieldAccess.EDIT.equals(fp.getAccess())) {
                editKeys.add(key);
            } else if (FieldAccess.VIEW.equals(fp.getAccess())) {
                viewRoles.computeIfAbsent(key, k -> new HashSet<>()).add(fp.getRoleId());
            }
        }
        Set<String> viewOnly = new HashSet<>();
        viewRoles.forEach((key, roles) -> {
            // 无任何角色给 edit，且每个角色都显式设了 view（覆盖全部角色）→ 只读
            if (!editKeys.contains(key) && roles.size() >= totalRoles) {
                viewOnly.add(key);
            }
        });
        return viewOnly;
    }

    /** 计算某部门的全部下属部门 ID（不含自身）。 */
    private List<Long> subDeptIds(Long deptId) {
        List<Long> result = new ArrayList<>();
        if (deptId == null) {
            return result;
        }
        List<SysDept> all = deptMapper.selectList(null);
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(deptId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            for (SysDept d : all) {
                if (current.equals(d.getParentId())) {
                    result.add(d.getId());
                    queue.add(d.getId());
                }
            }
        }
        return result;
    }
}
