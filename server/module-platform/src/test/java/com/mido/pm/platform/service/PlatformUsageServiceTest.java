package com.mido.pm.platform.service;

import com.mido.pm.platform.entity.SysTenant;
import com.mido.pm.platform.mapper.SysTenantMapper;
import com.mido.pm.platform.mapper.SysTenantQuotaUsageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/** 用量快照单测：批量快照逐租户独立、单租户失败不影响其余（P1-4）。 */
@ExtendWith(MockitoExtension.class)
class PlatformUsageServiceTest {

    @Mock
    private SysTenantMapper tenantMapper;
    @Mock
    private SysTenantQuotaUsageMapper usageMapper;
    @Mock
    private PlatformQuotaService quotaService;
    @Mock
    private PlatformUsageService self;

    private PlatformUsageService service() {
        return new PlatformUsageService(tenantMapper, usageMapper, quotaService, List.of(), self);
    }

    private SysTenant tenant(long id) {
        SysTenant t = new SysTenant();
        t.setId(id);
        t.setStatus("active");
        return t;
    }

    @Test
    void snapshotAllIsolatesPerTenantFailure() {
        when(tenantMapper.selectList(any())).thenReturn(List.of(tenant(2L), tenant(3L)));
        doThrow(new RuntimeException("boom")).when(self).snapshotTenant(2L);
        doNothing().when(self).snapshotTenant(3L);

        int ok = service().snapshotAll();

        // 租户2失败被隔离，租户3成功 → 成功数=1
        assertEquals(1, ok);
    }
}
