package com.mido.pm.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.project.ProjectExistenceGate;
import com.mido.pm.project.entity.PmProject;
import com.mido.pm.project.mapper.PmProjectMapper;
import org.springframework.stereotype.Service;

/**
 * {@link ProjectExistenceGate} 实现：查 pm_project 是否存在。租户条件由多租户拦截器自动注入，
 * 故跨租户的 projectId 在当前租户视角即「不存在」，天然拦截。
 */
@Service
public class ProjectExistenceGateImpl implements ProjectExistenceGate {

    private final PmProjectMapper projectMapper;

    public ProjectExistenceGateImpl(PmProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    @Override
    public void assertExists(Long projectId) {
        if (projectId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "projectId 不能为空");
        }
        Long count = projectMapper.selectCount(Wrappers.<PmProject>lambdaQuery()
                .eq(PmProject::getId, projectId));
        if (count == null || count == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        }
    }
}
