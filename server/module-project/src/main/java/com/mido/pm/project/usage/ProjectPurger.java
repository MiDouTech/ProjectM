package com.mido.pm.project.usage;

import com.mido.pm.common.tenant.TenantDataPurger;
import com.mido.pm.project.mapper.ProjectPurgeMapper;
import org.springframework.stereotype.Component;

/** 项目域数据清除。 */
@Component
public class ProjectPurger implements TenantDataPurger {

    private final ProjectPurgeMapper purgeMapper;

    public ProjectPurger(ProjectPurgeMapper purgeMapper) {
        this.purgeMapper = purgeMapper;
    }

    @Override
    public String domain() {
        return "project";
    }

    @Override
    public long purge(Long tenantId) {
        return purgeMapper.purgeMembers(tenantId) + purgeMapper.purgeProjects(tenantId);
    }
}
