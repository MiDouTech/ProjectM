package com.mido.pm.collab.purge;

import com.mido.pm.common.tenant.TenantDataPurger;
import org.springframework.stereotype.Component;

/** 协作(评论/通知)域数据清除。 */
@Component
public class CollabPurger implements TenantDataPurger {

    private final CollabPurgeMapper mapper;

    public CollabPurger(CollabPurgeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String domain() {
        return "collab";
    }

    @Override
    public long purge(Long tenantId) {
        return mapper.purgeNotifications(tenantId) + mapper.purgeComments(tenantId);
    }
}
