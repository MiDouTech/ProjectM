package com.mido.pm.platform.service;

import com.mido.pm.platform.entity.SysTenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 平台维护定时任务：处理待导出任务（高频）+ 清除到期注销租户（每日）。
 * cron 可经配置覆盖（@EnableScheduling 已在 Application 开启）。
 */
@Component
public class PlatformMaintenanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(PlatformMaintenanceScheduler.class);

    private final PlatformExportService exportService;
    private final PlatformDeletionService deletionService;
    private final TenantAdminService tenantAdminService;

    public PlatformMaintenanceScheduler(PlatformExportService exportService,
                                        PlatformDeletionService deletionService,
                                        TenantAdminService tenantAdminService) {
        this.exportService = exportService;
        this.deletionService = deletionService;
        this.tenantAdminService = tenantAdminService;
    }

    /** 到期租户自动流转为 expired（默认每日 01:30）。 */
    @Scheduled(cron = "${mido.platform.expire.cron:0 30 1 * * *}")
    public void expireOverdueTenants() {
        try {
            int n = tenantAdminService.expireOverdue();
            if (n > 0) {
                log.info("到期租户流转为 expired {} 个", n);
            }
        } catch (Exception e) {
            log.error("到期租户流转失败", e);
        }
    }

    /** 处理待导出任务（默认每分钟）。 */
    @Scheduled(fixedDelayString = "${mido.platform.export.poll-ms:60000}")
    public void processExports() {
        try {
            int n = exportService.processPending();
            if (n > 0) {
                log.info("处理导出任务 {} 个", n);
            }
        } catch (Exception e) {
            log.error("处理导出任务失败", e);
        }
    }

    /** 清除到期注销租户（默认每日 03:00）。逐租户独立事务调用，单个失败不影响其余。 */
    @Scheduled(cron = "${mido.platform.purge.cron:0 0 3 * * *}")
    public void purgeDue() {
        int n = 0;
        for (SysTenant tenant : deletionService.findDueTenants()) {
            try {
                // 经代理外部调用，使 purgeTenant 的 @Transactional 生效
                deletionService.purgeTenant(tenant);
                n++;
            } catch (Exception e) {
                log.error("清除到期注销租户失败 tenantId={}", tenant.getId(), e);
            }
        }
        if (n > 0) {
            log.info("清除到期注销租户 {} 个", n);
        }
    }
}
