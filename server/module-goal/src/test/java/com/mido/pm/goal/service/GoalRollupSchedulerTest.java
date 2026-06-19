package com.mido.pm.goal.service;

import com.mido.pm.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** 防抖调度：同项目短时多次登记合并为一次；工作线程内租户上下文被重建。 */
class GoalRollupSchedulerTest {

    @Test
    void coalescesBurstAndRestoresTenant() throws Exception {
        GoalRollupService svc = mock(GoalRollupService.class);
        AtomicReference<Long> tenantSeen = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            tenantSeen.set(TenantContext.get()); // 验证脱离请求线程后仍有租户
            latch.countDown();
            return null;
        }).when(svc).recomputeForProject(7L);

        GoalRollupScheduler scheduler = new GoalRollupScheduler(svc, 80);
        scheduler.schedule(9L, 7L); // 同项目连续 3 次 → 合并
        scheduler.schedule(9L, 7L);
        scheduler.schedule(9L, 7L);

        assertTrue(latch.await(2, TimeUnit.SECONDS), "应在静默窗口后触发一次重算");
        Thread.sleep(150); // 等待潜在的重复任务窗口过去
        verify(svc, times(1)).recomputeForProject(7L);
        assertEquals(9L, tenantSeen.get());
        scheduler.shutdown();
    }
}
