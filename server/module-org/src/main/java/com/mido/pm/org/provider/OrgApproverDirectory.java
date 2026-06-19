package com.mido.pm.org.provider;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.approval.domain.ApproverDirectory;
import com.mido.pm.org.entity.SysDept;
import com.mido.pm.org.entity.SysUser;
import com.mido.pm.org.entity.SysUserRole;
import com.mido.pm.org.mapper.SysDeptMapper;
import com.mido.pm.org.mapper.SysUserMapper;
import com.mido.pm.org.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 审批人目录的本地实现（读 sys_*）：为审批引擎的动态审批人解析提供组织能力。
 * 置于 module-org（持有 sys_* mapper），实现 approval 域定义的 {@link ApproverDirectory} 端口，分层不成环。
 */
@Component
public class OrgApproverDirectory implements ApproverDirectory {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysDeptMapper deptMapper;

    public OrgApproverDirectory(SysUserMapper userMapper, SysUserRoleMapper userRoleMapper,
                                SysDeptMapper deptMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.deptMapper = deptMapper;
    }

    @Override
    public List<Long> usersByRole(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        return userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getRoleId, roleId))
                .stream().map(SysUserRole::getUserId).distinct().toList();
    }

    @Override
    public Long deptLeaderOf(Long applicantId, int levelsUp) {
        if (applicantId == null) {
            return null;
        }
        SysUser applicant = userMapper.selectById(applicantId);
        if (applicant == null || applicant.getDeptId() == null) {
            return null;
        }
        // 层级 1=本部门，逐级沿 parent 上溯 levelsUp-1 次
        SysDept dept = deptMapper.selectById(applicant.getDeptId());
        for (int i = 1; i < Math.max(levelsUp, 1) && dept != null; i++) {
            Long parentId = dept.getParentId();
            dept = (parentId == null || parentId == 0L) ? null : deptMapper.selectById(parentId);
        }
        return dept == null ? null : dept.getLeaderId();
    }
}
