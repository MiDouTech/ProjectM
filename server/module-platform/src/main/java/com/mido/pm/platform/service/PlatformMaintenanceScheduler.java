package com.mido.pm.platform.service;

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

    public PlatformMaintenanceScheduler(PlatformExportService exportService,
                                        PlatformDeletionService deletionService) {
        this.exportService = exportService;
        this.deletionService = deletionService;
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

    /** 清除到期注销租户（默认每日 03:00）。 */
    @Scheduled(cron = "${mido.platform.purge.cron:0 0 3 * * *}")
    public void purgeDue() {
        try {
            int n = deletionService.purgeDue();
            if (n > 0) {
                log.info("清除到期注销租户 {} 个", n);
            }
        } catch (Exception e) {
            log.error("清除到期注销租户失败", e);
        }
    }
}
