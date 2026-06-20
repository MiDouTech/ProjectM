package com.mido.pm.project.usage;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.quota.QuotaResources;
import com.mido.pm.common.quota.UsageContributor;
import com.mido.pm.project.entity.PmProject;
import com.mido.pm.project.mapper.PmProjectMapper;
import org.springframework.stereotype.Component;

/** 用量贡献：当前租户项目数（经多租户拦截器按 TenantContext 隔离）。 */
@Component
public class ProjectUsageContributor implements UsageContributor {

    private final PmProjectMapper projectMapper;

    public ProjectUsageContributor(PmProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    @Override
    public String resource() {
        return QuotaResources.PROJECT;
    }

    @Override
    public long currentCount() {
        Long c = projectMapper.selectCount(Wrappers.<PmProject>lambdaQuery());
        return c == null ? 0L : c;
    }
}
