package com.mido.pm.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.audit.AuditActions;
import com.mido.pm.common.audit.Audited;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.project.dto.ProjectRoleSaveDTO;
import com.mido.pm.project.dto.ProjectRoleVO;
import com.mido.pm.project.entity.PmProjectRole;
import com.mido.pm.project.mapper.PmProjectRoleMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 项目角色服务：租户自配 CRUD。内置角色（builtin=1）不可删除；code 唯一。
 * 取代原 pm_project_member.project_role 自由文本，使「项目角色」成为可配置的第二级权限维度。
 */
@Service
public class ProjectRoleService {

    private static final String STATUS_ACTIVE = "active";

    private final PmProjectRoleMapper roleMapper;

    public ProjectRoleService(PmProjectRoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    /** 角色列表：onlyActive=true 仅启用；按 sort、id 升序。 */
    public List<ProjectRoleVO> list(boolean onlyActive) {
        return roleMapper.selectList(Wrappers.<PmProjectRole>lambdaQuery()
                        .eq(onlyActive, PmProjectRole::getStatus, STATUS_ACTIVE)
                        .orderByAsc(PmProjectRole::getSort).orderByAsc(PmProjectRole::getId))
                .stream().map(this::toVO).toList();
    }

    /** 某 code 是否为当前租户已配置的项目角色（供成员校验）。 */
    public boolean existsByCode(String code) {
        Long c = roleMapper.selectCount(Wrappers.<PmProjectRole>lambdaQuery()
                .eq(PmProjectRole::getCode, code));
        return c != null && c > 0;
    }

    /** 当前租户是否配置了任何项目角色（无则成员校验放行，保持向后兼容）。 */
    public boolean anyConfigured() {
        Long c = roleMapper.selectCount(Wrappers.<PmProjectRole>lambdaQuery());
        return c != null && c > 0;
    }

    @Audited(module = AuditActions.MODULE_CONFIG, action = AuditActions.CREATED, target = AuditActions.TARGET_PROJECT_ROLE)
    public Long create(ProjectRoleSaveDTO dto) {
        assertCodeUnique(dto.code());
        PmProjectRole r = new PmProjectRole();
        r.setCode(dto.code());
        r.setName(dto.name());
        r.setBuiltin(0);
        r.setSort(dto.sort() == null ? 0 : dto.sort());
        r.setStatus(dto.status() == null ? STATUS_ACTIVE : dto.status());
        roleMapper.insert(r);
        return r.getId();
    }

    /** 更新：code 不可改（以 id 为准），其余覆盖。 */
    @Audited(module = AuditActions.MODULE_CONFIG, action = AuditActions.UPDATED, target = AuditActions.TARGET_PROJECT_ROLE)
    public void update(Long id, ProjectRoleSaveDTO dto) {
        PmProjectRole r = requireExists(id);
        r.setName(dto.name());
        if (dto.sort() != null) {
            r.setSort(dto.sort());
        }
        if (dto.status() != null) {
            r.setStatus(dto.status());
        }
        roleMapper.updateById(r);
    }

    @Audited(module = AuditActions.MODULE_CONFIG, action = AuditActions.DELETED, target = AuditActions.TARGET_PROJECT_ROLE)
    public void delete(Long id) {
        PmProjectRole r = requireExists(id);
        if (r.getBuiltin() != null && r.getBuiltin() == 1) {
            throw new BizException(ErrorCode.CONFLICT, "内置项目角色不可删除");
        }
        roleMapper.deleteById(id);
    }

    private void assertCodeUnique(String code) {
        if (existsByCode(code)) {
            throw new BizException(ErrorCode.CONFLICT, "项目角色编码已存在");
        }
    }

    private PmProjectRole requireExists(Long id) {
        PmProjectRole r = roleMapper.selectById(id);
        if (r == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目角色不存在");
        }
        return r;
    }

    private ProjectRoleVO toVO(PmProjectRole r) {
        return new ProjectRoleVO(r.getId(), r.getCode(), r.getName(), r.getBuiltin(), r.getSort(), r.getStatus());
    }
}
