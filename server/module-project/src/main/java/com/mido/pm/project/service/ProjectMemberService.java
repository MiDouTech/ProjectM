package com.mido.pm.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.audit.AuditActions;
import com.mido.pm.common.audit.AuditLogService;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.project.dto.ProjectMemberCreateDTO;
import com.mido.pm.project.dto.ProjectMemberVO;
import com.mido.pm.project.entity.PmProjectMember;
import com.mido.pm.project.mapper.PmProjectMapper;
import com.mido.pm.project.mapper.PmProjectMemberMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目成员服务：增/查/删。
 */
@Service
public class ProjectMemberService {

    private final PmProjectMemberMapper memberMapper;
    private final PmProjectMapper projectMapper;
    private final AuditLogService auditLogService;
    private final ProjectRoleService projectRoleService;

    public ProjectMemberService(PmProjectMemberMapper memberMapper, PmProjectMapper projectMapper,
                                AuditLogService auditLogService, ProjectRoleService projectRoleService) {
        this.memberMapper = memberMapper;
        this.projectMapper = projectMapper;
        this.auditLogService = auditLogService;
        this.projectRoleService = projectRoleService;
    }

    public Long add(Long projectId, ProjectMemberCreateDTO dto) {
        requireProject(projectId);
        // 项目角色自定义化：若租户已配置项目角色，则成员角色须为已配置项 code（未配置则放行，兼容旧数据）
        if (dto.projectRole() != null && projectRoleService.anyConfigured()
                && !projectRoleService.existsByCode(dto.projectRole())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法项目角色: " + dto.projectRole());
        }
        PmProjectMember m = new PmProjectMember();
        m.setProjectId(projectId);
        m.setUserId(dto.userId());
        m.setProjectRole(dto.projectRole());
        memberMapper.insert(m);
        // 成员组织变动：target=project_member，entityId 取项目 ID，detail 记录成员与项目角色
        Map<String, Object> detail = new HashMap<>();
        detail.put("userId", dto.userId());
        detail.put("projectRole", dto.projectRole());
        auditLogService.record(AuditActions.MODULE_MEMBER, AuditActions.TARGET_PROJECT_MEMBER,
                projectId, AuditActions.MEMBER_ADDED, detail);
        return m.getId();
    }

    public List<ProjectMemberVO> list(Long projectId) {
        return memberMapper.selectList(
                        Wrappers.<PmProjectMember>lambdaQuery().eq(PmProjectMember::getProjectId, projectId))
                .stream()
                .map(m -> new ProjectMemberVO(m.getId(), m.getProjectId(), m.getUserId(), m.getProjectRole()))
                .toList();
    }

    public void remove(Long projectId, Long memberId) {
        PmProjectMember m = memberMapper.selectById(memberId);
        if (m == null || !projectId.equals(m.getProjectId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目成员不存在");
        }
        memberMapper.deleteById(memberId);
        Map<String, Object> detail = new HashMap<>();
        detail.put("userId", m.getUserId());
        detail.put("projectRole", m.getProjectRole());
        auditLogService.record(AuditActions.MODULE_MEMBER, AuditActions.TARGET_PROJECT_MEMBER,
                projectId, AuditActions.MEMBER_REMOVED, detail);
    }

    private void requireProject(Long projectId) {
        if (projectMapper.selectById(projectId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        }
    }
}
