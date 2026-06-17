package com.mido.pm.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.mido.pm.common.tenant.TenantContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 公共字段自动填充：create_time / update_time / create_by / update_by / tenant_id。
 * 审计人（createBy/updateBy）后续接入登录上下文，当前阶段留空由上层显式设置。
 */
@Component
public class MidoMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
        // 租户：兜底填充，多租户拦截器亦会注入，二者一致
        Long tenantId = TenantContext.get();
        if (tenantId != null) {
            this.strictInsertFill(metaObject, "tenantId", Long.class, tenantId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
