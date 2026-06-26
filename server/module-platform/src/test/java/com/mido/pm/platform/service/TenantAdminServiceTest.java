package com.mido.pm.platform.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.platform.dto.TenantCreateDTO;
import com.mido.pm.platform.dto.TenantStatusDTO;
import com.mido.pm.platform.entity.SysTenant;
import com.mido.pm.platform.mapper.SysTenantMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 租户管理服务单测（无 DB，mock mapper）。 */
@ExtendWith(MockitoExtension.class)
class TenantAdminServiceTest {

    @Mock
    private SysTenantMapper tenantMapper;
    @Mock
    private PlatformSubscriptionService subscriptionService;
    @Mock
    private PlatformPlanService planService;
    @Mock
    private PlatformAuditService auditService;

    private TenantAdminService service() {
        // 播种器列表传空：本单测聚焦租户行写入与审计，播种各域已由各自单测/联调覆盖。
        return new TenantAdminService(tenantMapper, subscriptionService, planService, auditService, java.util.List.of());
    }

    @Test
    void createDuplicateCodeRejected() {
        when(tenantMapper.selectCount(any())).thenReturn(1L);
        TenantCreateDTO dto = new TenantCreateDTO("米多", "mido", null, null, null, null, null, null, null);
        assertThrows(BizException.class, () -> service().create(dto));
    }

    @Test
    void createStartsAsTrialFromManualSource() {
        when(tenantMapper.selectCount(any())).thenReturn(0L);
        TenantCreateDTO dto = new TenantCreateDTO("新客户", "newco", "互联网", "张三", "13800000000", null, "备注", null, null);

        service().create(dto);

        ArgumentCaptor<SysTenant> captor = ArgumentCaptor.forClass(SysTenant.class);
        verify(tenantMapper).insert(captor.capture());
        assertEquals("trial", captor.getValue().getStatus());
        assertEquals("manual", captor.getValue().getSource());
        verify(auditService).record(any(), any(), any(), any());
    }

    @Test
    void changeStatusRejectsIllegalValue() {
        TenantStatusDTO dto = new TenantStatusDTO("running", null);
        assertThrows(BizException.class, () -> service().changeStatus(1L, dto));
    }

    @Test
    void changeStatusSuspendsTenant() {
        SysTenant t = new SysTenant();
        t.setId(1L);
        t.setStatus("active");
        when(tenantMapper.selectById(1L)).thenReturn(t);

        service().changeStatus(1L, new TenantStatusDTO("suspended", "欠费"));

        assertEquals("suspended", t.getStatus());
        verify(tenantMapper).updateById(t);
        verify(auditService).record(any(), any(), any(), any());
    }
}
