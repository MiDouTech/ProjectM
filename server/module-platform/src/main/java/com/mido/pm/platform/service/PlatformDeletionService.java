package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.tenant.TenantContext;
import com.mido.pm.common.tenant.TenantDataPurger;
import com.mido.pm.platform.entity.SysTenant;
import com.mido.pm.platform.mapper.SysTenantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户注销合规：运营发起注销→标记 closed + 计划清除时间(默认30天宽限)；宽限期内可取消。
 * 定时任务对到期租户调用各域 {@link TenantDataPurger} 物理清除数据，标记 purged。
 * 安全护栏：自用租户(DEFAULT_TENANT_ID)永不注销/清除。
 */
@Service
public class PlatformDeletionService {

    private static final Logger log = LoggerFactory.getLogger(PlatformDeletionService.class);

    private final SysTenantMapper tenantMapper;
    private final List<TenantDataPurger> purgers;
    private final PlatformAuditService auditService;
    private final int defaultGraceDays;

    public PlatformDeletionService(SysTenantMapper tenantMapper, List<TenantDataPurger> purgers,
                                   PlatformAuditService auditService,
                                   @Value("${mido.platform.purge.grace-days:30}") int defaultGraceDays) {
        this.tenantMapper = tenantMapper;
        this.purgers = purgers;
        this.auditService = auditService;
        this.defaultGraceDays = defaultGraceDays;
    }

    /** 发起注销：标记 closed，安排 graceDays 后清除。 */
    @Transactional(rollbackFor = Exception.class)
    public void requestDeletion(Long tenantId, Integer graceDays) {
        guardNotSelfUse(tenantId);
        SysTenant tenant = requireExists(tenantId);
        int grace = graceDays == null || graceDays < 0 ? defaultGraceDays : graceDays;
        LocalDateTime purgeAt = LocalDateTime.now().plusDays(grace);
        tenant.setStatus("closed");
        tenant.setPurgeScheduledAt(purgeAt);
        tenantMapper.updateById(tenant);
        auditService.record(PlatformAuditActions.TENANT_DELETION_REQUESTED,
                PlatformAuditActions.TARGET_TENANT, tenantId, Map.of("purgeAt", purgeAt.toString()));
    }

    /** 宽限期内取消注销：清除计划，状态置 suspended（由运营再决定恢复）。 */
    @Transactional(rollbackFor = Exception.class)
    public void cancelDeletion(Long tenantId) {
        SysTenant tenant = requireExists(tenantId);
        if (tenant.getPurgeScheduledAt() == null) {
            throw new BizException(ErrorCode.CONFLICT, "该租户未处于注销流程");
        }
        tenant.setPurgeScheduledAt(null);
        tenant.setStatus("suspended");
        tenantMapper.updateById(tenant);
        auditService.record(PlatformAuditActions.TENANT_DELETION_CANCELLED,
                PlatformAuditActions.TARGET_TENANT, tenantId, null);
    }

    /** 定时清除到期注销租户（closed 且 purge_scheduled_at<=now），物理删除各域数据后标记 purged。 */
    public int purgeDue() {
        LocalDateTime now = LocalDateTime.now();
        List<SysTenant> due = tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery()
                .eq(SysTenant::getStatus, "closed")
                .isNotNull(SysTenant::getPurgeScheduledAt)
                .le(SysTenant::getPurgeScheduledAt, now));
        int purged = 0;
        for (SysTenant tenant : due) {
            if (tenant.getId() != null && tenant.getId() == TenantContext.DEFAULT_TENANT_ID) {
                continue;
            }
            purgeTenant(tenant);
            purged++;
        }
        return purged;
    }

    @Transactional(rollbackFor = Exception.class)
    public void purgeTenant(SysTenant tenant) {
        Map<String, Object> detail = new HashMap<>();
        long total = 0;
        for (TenantDataPurger purger : purgers) {
            long n = purger.purge(tenant.getId());
            detail.put(purger.domain(), n);
            total += n;
        }
        tenant.setStatus("purged");
        tenant.setPurgeScheduledAt(null);
        tenantMapper.updateById(tenant);
        detail.put("total", total);
        auditService.record(PlatformAuditActions.TENANT_PURGED,
                PlatformAuditActions.TARGET_TENANT, tenant.getId(), detail);
        log.info("租户已清除 tenantId={} 删除行数={}", tenant.getId(), total);
    }

    private void guardNotSelfUse(Long tenantId) {
        if (tenantId != null && tenantId == TenantContext.DEFAULT_TENANT_ID) {
            throw new BizException(ErrorCode.FORBIDDEN, "自用租户不可注销");
        }
    }

    private SysTenant requireExists(Long tenantId) {
        SysTenant t = tenantMapper.selectById(tenantId);
        if (t == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "租户不存在");
        }
        return t;
    }
}
