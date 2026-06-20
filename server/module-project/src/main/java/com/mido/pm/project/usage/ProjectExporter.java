package com.mido.pm.project.usage;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.tenant.TenantDataExporter;
import com.mido.pm.project.entity.PmProject;
import com.mido.pm.project.mapper.PmProjectMapper;
import org.springframework.stereotype.Component;

/** 导出当前租户项目数据。 */
@Component
public class ProjectExporter implements TenantDataExporter {

    private final PmProjectMapper projectMapper;

    public ProjectExporter(PmProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    @Override
    public String domain() {
        return "projects";
    }

    @Override
    public Object exportData() {
        return projectMapper.selectList(Wrappers.<PmProject>lambdaQuery());
    }
}
