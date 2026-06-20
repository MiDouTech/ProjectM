package com.mido.pm.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 用量快照定时任务：每日凌晨对全部未注销租户做一次用量统计快照（@EnableScheduling 已在 Application 开启）。
 * cron 可经 mido.platform.usage.snapshot-cron 覆盖。
 */
@Component
public class PlatformUsageScheduler {

    private static final Logger log = LoggerFactory.getLogger(PlatformUsageScheduler.class);

    private final PlatformUsageService usageService;

    public PlatformUsageScheduler(PlatformUsageService usageService) {
        this.usageService = usageService;
    }

    @Scheduled(cron = "${mido.platform.usage.snapshot-cron:0 0 2 * * *}")
    public void snapshot() {
        try {
            int count = usageService.snapshotAll();
            log.info("租户用量快照完成，处理租户数={}", count);
        } catch (Exception e) {
            log.error("租户用量快照失败", e);
        }
    }
}
