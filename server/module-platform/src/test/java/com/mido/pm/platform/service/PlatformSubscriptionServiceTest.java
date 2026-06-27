package com.mido.pm.platform.service;

import com.mido.pm.platform.dto.SubscriptionSaveDTO;
import com.mido.pm.platform.entity.SysPlan;
import com.mido.pm.platform.entity.SysTenant;
import com.mido.pm.platform.entity.SysTenantSubscription;
import com.mido.pm.platform.mapper.SysPlanMapper;
import com.mido.pm.platform.mapper.SysTenantMapper;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.platform.mapper.SysTenantSubscriptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 租户订阅服务单测：验证绑定订阅时同步租户状态/到期/激活时间，并作废旧订阅。 */
@ExtendWith(MockitoExtension.class)
class PlatformSubscriptionServiceTest {

    @Mock
    private SysTenantSubscriptionMapper subscriptionMapper;
    @Mock
    private SysTenantMapper tenantMapper;
    @Mock
    private SysPlanMapper planMapper;
    @Mock
    private PlatformAuditService auditService;
    @Mock
    private PlatformUsageService usageService;
    @Mock
    private com.mido.pm.common.outbox.DomainEventPublisher eventPublisher;

    private PlatformSubscriptionService service() {
        return new PlatformSubscriptionService(subscriptionMapper, tenantMapper, planMapper, auditService,
                usageService, eventPublisher);
    }

    @Test
    void bindCancelsOldActiveAndSyncsTenant() {
        SysTenant tenant = new SysTenant();
        tenant.setId(1L);
        tenant.setStatus("trial");
        tenant.setActivatedAt(null);
        when(tenantMapper.selectById(1L)).thenReturn(tenant);
        SysPlan plan = new SysPlan();
        plan.setId(3L);
        plan.setName("旗舰版");
        when(planMapper.selectById(3L)).thenReturn(plan);

        LocalDateTime expire = LocalDateTime.now().plusYears(1);
        service().bind(1L, new SubscriptionSaveDTO(3L, null, expire, "首单"));

        // 旧 active 订阅被作废
        verify(subscriptionMapper).update(any(SysTenantSubscription.class), any());
        // 新订阅落库
        verify(subscriptionMapper).insert(any(SysTenantSubscription.class));
        // 租户同步：状态 active、到期=订阅到期、首次激活时间被填充
        ArgumentCaptor<SysTenant> captor = ArgumentCaptor.forClass(SysTenant.class);
        verify(tenantMapper).updateById(captor.capture());
        assertEquals("active", captor.getValue().getStatus());
        assertEquals(expire, captor.getValue().getExpireAt());
        assertNotNull(captor.getValue().getActivatedAt());
        verify(auditService).record(any(), any(), any(), any());
    }

    @Test
    void concurrentBindHittingUniqueKeyTranslatedToConflict() {
        SysTenant tenant = new SysTenant();
        tenant.setId(1L);
        tenant.setStatus("trial");
        when(tenantMapper.selectById(1L)).thenReturn(tenant);
        SysPlan plan = new SysPlan();
        plan.setId(3L);
        when(planMapper.selectById(3L)).thenReturn(plan);
        // 模拟并发命中 uk_sub_active_tenant 唯一约束
        when(subscriptionMapper.insert(any(SysTenantSubscription.class)))
                .thenThrow(new DuplicateKeyException("uk_sub_active_tenant"));

        assertThrows(BizException.class,
                () -> service().bind(1L, new SubscriptionSaveDTO(3L, null, null, null)));
    }

    @Test
    void bindWithExistingOverQuotaRecordsWarning() {
        SysTenant tenant = new SysTenant();
        tenant.setId(1L);
        tenant.setStatus("active");
        when(tenantMapper.selectById(1L)).thenReturn(tenant);
        SysPlan plan = new SysPlan();
        plan.setId(2L);
        plan.setName("标准版");
        when(planMapper.selectById(2L)).thenReturn(plan);
        // 降级后存量超额
        when(usageService.overQuotaResources(1L)).thenReturn(List.of("user"));

        service().bind(1L, new SubscriptionSaveDTO(2L, null, null, null));

        verify(auditService).record(eq(PlatformAuditActions.SUBSCRIPTION_OVERQUOTA), any(), eq(1L), any());
    }
}
