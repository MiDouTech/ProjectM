package com.mido.pm.view.purge;

import com.mido.pm.common.tenant.TenantDataPurger;
import org.springframework.stereotype.Component;

/** 视图/页面/导航配置域数据清除。 */
@Component
public class ViewPurger implements TenantDataPurger {

    private final ViewPurgeMapper mapper;

    public ViewPurger(ViewPurgeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String domain() {
        return "view";
    }

    @Override
    public long purge(Long tenantId) {
        return mapper.purgePageConfigs(tenantId)
                + mapper.purgeViews(tenantId)
                + mapper.purgeModuleNavs(tenantId);
    }
}
