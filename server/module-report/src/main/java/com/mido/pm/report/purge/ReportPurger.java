package com.mido.pm.report.purge;

import com.mido.pm.common.tenant.TenantDataPurger;
import org.springframework.stereotype.Component;

/** 报表(PMO度量配置)域数据清除。 */
@Component
public class ReportPurger implements TenantDataPurger {

    private final ReportPurgeMapper mapper;

    public ReportPurger(ReportPurgeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String domain() {
        return "report";
    }

    @Override
    public long purge(Long tenantId) {
        return mapper.purgeSettings(tenantId);
    }
}
