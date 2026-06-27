package com.mido.pm.field.purge;

import com.mido.pm.common.tenant.TenantDataPurger;
import org.springframework.stereotype.Component;

/** 自定义字段/数据源域数据清除。 */
@Component
public class FieldPurger implements TenantDataPurger {

    private final FieldPurgeMapper mapper;

    public FieldPurger(FieldPurgeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String domain() {
        return "field";
    }

    @Override
    public long purge(Long tenantId) {
        return mapper.purgeFieldValues(tenantId)
                + mapper.purgeDataSourceOptions(tenantId)
                + mapper.purgeDataSources(tenantId)
                + mapper.purgeFieldDefs(tenantId);
    }
}
