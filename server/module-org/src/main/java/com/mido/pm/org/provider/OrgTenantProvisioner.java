package com.mido.pm.org.provider;

import com.mido.pm.common.tenant.TenantProvisionContext;
import com.mido.pm.common.tenant.TenantProvisioner;
import com.mido.pm.org.entity.SysDept;
import com.mido.pm.org.entity.SysRole;
import com.mido.pm.org.entity.SysRoleDataScope;
import com.mido.pm.org.entity.SysRolePerm;
import com.mido.pm.org.entity.SysUser;
import com.mido.pm.org.entity.SysUserRole;
import com.mido.pm.org.mapper.SysDeptMapper;
import com.mido.pm.org.mapper.SysRoleDataScopeMapper;
import com.mido.pm.org.mapper.SysRolePermMapper;
import com.mido.pm.org.mapper.SysRoleMapper;
import com.mido.pm.org.mapper.SysUserMapper;
import com.mido.pm.org.mapper.SysUserRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 组织域租户播种（order=10，最先）：为新租户建「总部」部门、超级管理员角色（全量功能权限 + user 全数据范围）、
 * 管理员账号（凭据取自开通入参），使新租户开通即可登录管理。生成的 adminUserId/adminRoleId 写入共享袋，
 * 供后序域（审批流审批人、…）引用。
 */
@Component
public class OrgTenantProvisioner implements TenantProvisioner {

    /** 超级管理员功能权限码（与各 module-org 控制器 @PreAuthorize 对齐；含租户级审计查询）。 */
    private static final List<String> ADMIN_PERMS = List.of(
            "org:user:query", "org:user:create", "org:role:create", "org:dept:create", "org:audit:query",
            "org:config:manage");

    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermMapper rolePermMapper;
    private final SysRoleDataScopeMapper roleDataScopeMapper;
    private final PasswordEncoder passwordEncoder;

    public OrgTenantProvisioner(SysDeptMapper deptMapper, SysRoleMapper roleMapper, SysUserMapper userMapper,
                                SysUserRoleMapper userRoleMapper, SysRolePermMapper rolePermMapper,
                                SysRoleDataScopeMapper roleDataScopeMapper, PasswordEncoder passwordEncoder) {
        this.deptMapper = deptMapper;
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermMapper = rolePermMapper;
        this.roleDataScopeMapper = roleDataScopeMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public void provision(TenantProvisionContext ctx) {
        SysDept dept = new SysDept();
        dept.setName("总部");
        dept.setParentId(0L);
        deptMapper.insert(dept);

        SysRole role = new SysRole();
        role.setName("超级管理员");
        role.setCode("admin");
        roleMapper.insert(role);

        for (String perm : ADMIN_PERMS) {
            SysRolePerm rp = new SysRolePerm();
            rp.setRoleId(role.getId());
            rp.setPermCode(perm);
            rolePermMapper.insert(rp);
        }

        SysRoleDataScope scope = new SysRoleDataScope();
        scope.setRoleId(role.getId());
        scope.setResource("user");
        scope.setScope("all");
        roleDataScopeMapper.insert(scope);

        SysUser admin = new SysUser();
        admin.setUsername(ctx.adminUsername());
        admin.setName("管理员");
        admin.setPassword(passwordEncoder.encode(ctx.adminPassword()));
        admin.setDeptId(dept.getId());
        admin.setJobLevel("L3");
        admin.setStatus("active");
        userMapper.insert(admin);

        SysUserRole ur = new SysUserRole();
        ur.setUserId(admin.getId());
        ur.setRoleId(role.getId());
        userRoleMapper.insert(ur);

        ctx.put(TenantProvisionContext.KEY_ADMIN_USER_ID, admin.getId());
        ctx.put(TenantProvisionContext.KEY_ADMIN_ROLE_ID, role.getId());
    }
}
