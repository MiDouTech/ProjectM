package com.mido.pm.platform.service;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.quota.QuotaResources;
import com.mido.pm.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** 配额硬校验单测：上限/不限/无租户上下文。 */
@ExtendWith(MockitoExtension.class)
class PlatformQuotaGuardTest {

    @Mock
    private PlatformQuotaService quotaService;
    @InjectMocks
    private PlatformQuotaGuard guard;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void noTenantContextSkips() {
        TenantContext.clear();
        assertDoesNotThrow(() -> guard.checkCanAdd(QuotaResources.USER, 999));
    }

    @Test
    void unlimitedAllows() {
        TenantContext.set(1L);
        when(quotaService.effectiveLimit(1L, QuotaResources.USER)).thenReturn(-1L);
        assertDoesNotThrow(() -> guard.checkCanAdd(QuotaResources.USER, 9999));
    }

    @Test
    void withinLimitAllows() {
        TenantContext.set(1L);
        lenient().when(quotaService.effectiveLimit(1L, QuotaResources.USER)).thenReturn(10L);
        assertDoesNotThrow(() -> guard.checkCanAdd(QuotaResources.USER, 5));
    }

    @Test
    void overLimitThrows() {
        TenantContext.set(1L);
        when(quotaService.effectiveLimit(1L, QuotaResources.USER)).thenReturn(10L);
        // 已有 10 个，再加 1 = 11 > 10 → 超限
        assertThrows(BizException.class, () -> guard.checkCanAdd(QuotaResources.USER, 10));
    }
}
