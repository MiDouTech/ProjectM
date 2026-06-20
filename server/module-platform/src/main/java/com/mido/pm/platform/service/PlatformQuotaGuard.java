package com.mido.pm.platform.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.quota.QuotaGuard;
import com.mido.pm.common.tenant.TenantContext;
import org.springframework.stereotype.Component;

/**
 * QuotaGuard 端口的平台域实现：按当前 TenantContext 租户的生效配额硬校验。
 * 上限 -1（不限）或无生效订阅时放行。
 */
@Component
public class PlatformQuotaGuard implements QuotaGuard {

    private final PlatformQuotaService quotaService;

    public PlatformQuotaGuard(PlatformQuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @Override
    public void checkCanAdd(String resource, long currentCount) {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            return;
        }
        long limit = quotaService.effectiveLimit(tenantId, resource);
        if (limit < 0) {
            return;
        }
        if (currentCount + 1 > limit) {
            throw new BizException(ErrorCode.CONFLICT,
                    "已达套餐配额上限（" + resource + "：" + limit + "），请升级套餐");
        }
    }
}
