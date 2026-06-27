package com.mido.pm.platform.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.tenant.TenantContext;
import com.mido.pm.common.tenant.TenantUserLocator;
import com.mido.pm.platform.dto.ImpersonateVO;
import com.mido.pm.platform.entity.SysTenant;
import com.mido.pm.platform.mapper.SysTenantMapper;
import com.mido.pm.platform.security.PlatformContext;
import com.mido.pm.provider.sso.SsoProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 模拟登录：平台运营以目标租户某用户身份签发短时租户令牌进入租户应用排障（完整权限）。
 * 高敏感操作——目标用户优先取租户管理员，否则取该租户最早 active 用户；全程审计。
 */
@Service
public class PlatformImpersonationService {

    private final SysTenantMapper tenantMapper;
    private final TenantUserLocator tenantUserLocator;
    private final SsoProvider ssoProvider;
    private final PlatformAuditService auditService;
    private final long impersonationTtlMillis;

    public PlatformImpersonationService(SysTenantMapper tenantMapper, TenantUserLocator tenantUserLocator,
                                        SsoProvider ssoProvider, PlatformAuditService auditService,
                                        @Value("${mido.jwt.impersonation-ttl-millis:1800000}") long impersonationTtlMillis) {
        this.tenantMapper = tenantMapper;
        this.tenantUserLocator = tenantUserLocator;
        this.ssoProvider = ssoProvider;
        this.auditService = auditService;
        this.impersonationTtlMillis = impersonationTtlMillis;
    }

    @Transactional(rollbackFor = Exception.class)
    public ImpersonateVO impersonate(Long tenantId) {
        // 自用租户(平台自身)不可被模拟，与注销护栏一致
        if (tenantId != null && tenantId == TenantContext.DEFAULT_TENANT_ID) {
            throw new BizException(ErrorCode.FORBIDDEN, "自用租户不可模拟登录");
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "租户不存在");
        }
        Long targetUserId = tenant.getAdminUserId() != null
                ? tenant.getAdminUserId() : tenantUserLocator.primaryUserId(tenantId);
        if (targetUserId == null) {
            throw new BizException(ErrorCode.CONFLICT, "该租户暂无可用用户，无法模拟登录");
        }
        Long adminId = PlatformContext.currentAdminId();
        String token = ssoProvider.issueImpersonationToken(targetUserId, tenantId, adminId);

        Map<String, Object> detail = new HashMap<>();
        detail.put("tenantCode", tenant.getCode());
        detail.put("targetUserId", targetUserId);
        auditService.record(PlatformAuditActions.TENANT_IMPERSONATED,
                PlatformAuditActions.TARGET_TENANT, tenantId, detail);

        return new ImpersonateVO(token, "Bearer", impersonationTtlMillis / 1000,
                tenantId, tenant.getCode(), targetUserId);
    }
}
