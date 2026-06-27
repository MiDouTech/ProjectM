package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.outbox.DomainEventPublisher;
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
    private final PlatformExportService exportService;
    private final DomainEventPublisher eventPublisher;
    private final int defaultGraceDays;

    public PlatformDeletionService(SysTenantMapper tenantMapper, List<TenantDataPurger> purgers,
                                   PlatformAuditService auditService,
                                   PlatformExportService exportService,
                                   DomainEventPublisher eventPublisher,
                                   @Value("${mido.platform.purge.grace-days:30}") int defaultGraceDays) {
        this.tenantMapper = tenantMapper;
        this.purgers = purgers;
        this.auditService = auditService;
        this.exportService = exportService;
        this.eventPublisher = eventPublisher;
        this.defaultGraceDays = defaultGraceDays;
    }

    /** 发起注销：标记 closed，安排 graceDays(至少1天) 后清除，并自动发起一次数据导出作为清除前备份。 */
    @Transactional(rollbackFor = Exception.class)
    public void requestDeletion(Long tenantId, Integer graceDays) {
        guardNotSelfUse(tenantId);
        SysTenant tenant = requireExists(tenantId);
        // 宽限期至少 1 天，确保导出有时间完成、客户有缓冲
        int grace = graceDays == null || graceDays < 1 ? defaultGraceDays : graceDays;
        LocalDateTime purgeAt = LocalDateTime.now().plusDays(grace);
        tenant.setStatus("closed");
        tenant.setPurgeScheduledAt(purgeAt);
        tenantMapper.updateById(tenant);
        // 自动发起导出，作为物理清除前的合规备份（清除时强校验已完成导出）
        exportService.requestExport(tenantId);
        auditService.record(PlatformAuditActions.TENANT_DELETION_REQUESTED,
                PlatformAuditActions.TARGET_TENANT, tenantId, Map.of("purgeAt", purgeAt.toString()));
        eventPublisher.publish(PlatformEvents.TENANT_DELETION_REQUESTED, tenantId,
                Map.of("tenantId", tenantId, "purgeAt", purgeAt.toString()));
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

    /**
     * 找出到期注销租户（closed 且 purge_scheduled_at<=now，排除自用租户）。
     * 注意：清除按租户逐个事务执行，由调度器外部调用 {@link #purgeTenant}（经代理使 @Transactional 生效，
     * 且单租户失败不影响其余）；勿在本类内部直接循环调用 purgeTenant（自调用会绕过事务）。
     */
    public List<SysTenant> findDueTenants() {
        LocalDateTime now = LocalDateTime.now();
        return tenantMapper.selectList(Wrappers.<SysTenant>lambdaQuery()
                        .eq(SysTenant::getStatus, "closed")
                        .isNotNull(SysTenant::getPurgeScheduledAt)
                        .le(SysTenant::getPurgeScheduledAt, now))
                .stream()
                .filter(t -> !(t.getId() != null && t.getId() == TenantContext.DEFAULT_TENANT_ID))
                .toList();
    }

    /** 物理清除单个租户数据并标记 purged（独立事务）。自用租户防御性跳过。 */
    @Transactional(rollbackFor = Exception.class)
    public void purgeTenant(SysTenant tenant) {
        if (tenant.getId() != null && tenant.getId() == TenantContext.DEFAULT_TENANT_ID) {
            return;
        }
        // 合规护栏：无已完成导出不得物理清除（保留数据，待下次调度重试），避免无备份的不可逆删除
        if (!exportService.hasCompletedExport(tenant.getId())) {
            auditService.record(PlatformAuditActions.TENANT_PURGE_SKIPPED,
                    PlatformAuditActions.TARGET_TENANT, tenant.getId(), Map.of("reason", "no_completed_export"));
            log.warn("跳过清除：租户无已完成导出 tenantId={}", tenant.getId());
            return;
        }
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
        eventPublisher.publish(PlatformEvents.TENANT_PURGED, tenant.getId(),
                Map.of("tenantId", tenant.getId(), "total", total));
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
