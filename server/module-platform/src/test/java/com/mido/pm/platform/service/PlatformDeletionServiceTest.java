package com.mido.pm.platform.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.tenant.TenantDataPurger;
import com.mido.pm.platform.entity.SysTenant;
import com.mido.pm.platform.mapper.SysTenantMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 注销合规服务单测：自用租户护栏、计划清除、取消、物理清除编排。 */
@ExtendWith(MockitoExtension.class)
class PlatformDeletionServiceTest {

    @Mock
    private SysTenantMapper tenantMapper;
    @Mock
    private TenantDataPurger purger;
    @Mock
    private PlatformAuditService auditService;
    @Mock
    private PlatformExportService exportService;
    @Mock
    private com.mido.pm.common.outbox.DomainEventPublisher eventPublisher;

    private PlatformDeletionService service() {
        return new PlatformDeletionService(tenantMapper, List.of(purger), auditService, exportService, eventPublisher, 30);
    }

    private SysTenant tenant(Long id, LocalDateTime purgeAt) {
        SysTenant t = new SysTenant();
        t.setId(id);
        t.setStatus("active");
        t.setPurgeScheduledAt(purgeAt);
        return t;
    }

    @Test
    void selfUseTenantCannotBeDeleted() {
        assertThrows(BizException.class, () -> service().requestDeletion(1L, null));
    }

    @Test
    void requestDeletionMarksClosedAndSchedules() {
        SysTenant t = tenant(2L, null);
        when(tenantMapper.selectById(2L)).thenReturn(t);

        service().requestDeletion(2L, null);

        assertEquals("closed", t.getStatus());
        assertNotNull(t.getPurgeScheduledAt());
        verify(tenantMapper).updateById(t);
        // 注销自动发起一次导出作为清除前备份
        verify(exportService).requestExport(2L);
        verify(auditService).record(any(), any(), any(), any());
    }

    @Test
    void cancelWithoutPendingThrows() {
        when(tenantMapper.selectById(2L)).thenReturn(tenant(2L, null));
        assertThrows(BizException.class, () -> service().cancelDeletion(2L));
    }

    @Test
    void cancelClearsSchedule() {
        SysTenant t = tenant(2L, LocalDateTime.now().plusDays(10));
        when(tenantMapper.selectById(2L)).thenReturn(t);

        service().cancelDeletion(2L);

        assertNull(t.getPurgeScheduledAt());
        assertEquals("suspended", t.getStatus());
    }

    @Test
    void purgeTenantInvokesPurgersAndMarksPurged() {
        SysTenant t = tenant(2L, LocalDateTime.now());
        t.setStatus("closed");
        when(exportService.hasCompletedExport(2L)).thenReturn(true);
        when(purger.domain()).thenReturn("org");
        when(purger.purge(2L)).thenReturn(7L);

        service().purgeTenant(t);

        assertEquals("purged", t.getStatus());
        assertNull(t.getPurgeScheduledAt());
        verify(purger).purge(2L);
        verify(tenantMapper).updateById(t);
        verify(auditService).record(any(), any(), any(), any());
    }

    @Test
    void purgeSkippedWhenNoCompletedExport() {
        SysTenant t = tenant(2L, LocalDateTime.now());
        t.setStatus("closed");
        when(exportService.hasCompletedExport(2L)).thenReturn(false);

        service().purgeTenant(t);

        // 无已完成导出：不清除、不改状态，仅记跳过审计
        assertEquals("closed", t.getStatus());
        verify(purger, never()).purge(any());
        verify(auditService).record(eq(PlatformAuditActions.TENANT_PURGE_SKIPPED), any(), eq(2L), any());
    }
}
