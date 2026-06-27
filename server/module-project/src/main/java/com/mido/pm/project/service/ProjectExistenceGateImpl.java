package com.mido.pm.project.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.project.ProjectExistenceGate;
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
        // selectById 同样经多租户拦截器注入 tenant 条件，跨租户的 id 在当前租户视角即「不存在」
        if (projectMapper.selectById(projectId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "项目不存在");
        }
    }
}
