package com.mido.pm.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.project.dto.ProjectMemberCreateDTO;
import com.mido.pm.project.dto.ProjectMemberVO;
import com.mido.pm.project.entity.PmProjectMember;
import com.mido.pm.project.mapper.PmProjectMapper;
import com.mido.pm.project.mapper.PmProjectMemberMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 项目成员服务：增/查/删。
 */
@Service
public class ProjectMemberService {

    private final PmProjectMemberMapper memberMapper;
    private final PmProjectMapper projectMapper;

    public ProjectMemberService(PmProjectMemberMapper memberMapper, PmProjectMapper projectMapper) {
        this.memberMapper = memberMapper;
        this.projectMapper = projectMapper;
    }

    public Long add(Long projectId, ProjectMemberCreateDTO dto) {
        requireProject(projectId);
        PmProjectMember m = new PmProjectMember();
        m.setProjectId(projectId);
        m.setUserId(dto.userId());
        m.setProjectRole(dto.projectRole());
        memberMapper.insert(m);
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
    }

    private void requireProject(Long projectId) {
        if (projectMapper.selectById(projectId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        }
    }
}
