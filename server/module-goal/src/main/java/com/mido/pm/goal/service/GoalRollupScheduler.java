package com.mido.pm.goal.service;

import com.mido.pm.common.tenant.TenantContext;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 目标自动汇总防抖：项目下任务状态高频变动（如看板连续拖拽）时，按 projectId 合并，
 * 仅在静默 {@code delayMs} 后重算一次，避免每次状态变更都全量重算 KR。
 * 工作线程脱离请求上下文，需重建 {@link TenantContext}（否则多租户过滤失效）。
 * 进程内单实例（阶段一单体）；多实例分片后由 MQ 分区消费替代。
 */
@Component
public class GoalRollupScheduler {

    private static final Logger log = LoggerFactory.getLogger(GoalRollupScheduler.class);

    private final GoalRollupService rollupService;
    private final long delayMs;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "goal-rollup");
        t.setDaemon(true);
        return t;
    });
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> tenantOf = new ConcurrentHashMap<>();

    public GoalRollupScheduler(GoalRollupService rollupService,
                               @Value("${mido.goal.rollup-debounce-ms:1500}") long delayMs) {
        this.rollupService = rollupService;
        this.delayMs = delayMs;
    }

    /** 登记一次某项目的重算需求；同项目多次调用在静默窗口内合并为一次。 */
    public void schedule(Long tenantId, Long projectId) {
        if (projectId == null) {
            return;
        }
        tenantOf.put(projectId, tenantId == null ? TenantContext.DEFAULT_TENANT_ID : tenantId);
        pending.compute(projectId, (k, prev) -> {
            if (prev != null) {
                prev.cancel(false); // 取消尚未触发的旧任务 → 合并
            }
            return executor.schedule(() -> run(k), delayMs, TimeUnit.MILLISECONDS);
        });
    }

    private void run(Long projectId) {
        pending.remove(projectId);
        Long tenantId = tenantOf.remove(projectId);
        try {
            TenantContext.set(tenantId);
            rollupService.recomputeForProject(projectId);
        } catch (Exception e) {
            log.warn("目标自动汇总失败 projectId={}: {}", projectId, e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
