package com.mido.pm.platform.service;

import com.mido.pm.common.feature.FeatureCodes;
import com.mido.pm.platform.entity.SysPlanFeature;
import com.mido.pm.platform.entity.SysTenantSubscription;
import com.mido.pm.platform.mapper.SysPlanFeatureMapper;
import com.mido.pm.platform.mapper.SysTenantSubscriptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 功能开关服务单测：未订阅/未配置 fail-open；已配置按 enabled 过滤。 */
@ExtendWith(MockitoExtension.class)
class PlatformFeatureServiceTest {

    @Mock
    private SysPlanFeatureMapper planFeatureMapper;
    @Mock
    private SysTenantSubscriptionMapper subscriptionMapper;
    @Mock
    private PlatformAuditService auditService;
    @InjectMocks
    private PlatformFeatureService service;

    private SysTenantSubscription sub(Long planId) {
        SysTenantSubscription s = new SysTenantSubscription();
        s.setPlanId(planId);
        s.setStatus("active");
        return s;
    }

    private SysPlanFeature feature(String code, int enabled) {
        SysPlanFeature f = new SysPlanFeature();
        f.setFeatureCode(code);
        f.setEnabled(enabled);
        return f;
    }

    @Test
    void noSubscriptionReturnsAll() {
        when(subscriptionMapper.selectOne(any())).thenReturn(null);
        assertEquals(FeatureCodes.ALL, service.enabledFeaturesForTenant(1L));
    }

    @Test
    void emptyPlanFeaturesReturnsAll() {
        when(subscriptionMapper.selectOne(any())).thenReturn(sub(2L));
        when(planFeatureMapper.selectList(any())).thenReturn(List.of());
        assertEquals(FeatureCodes.ALL, service.enabledFeaturesForTenant(1L));
    }

    @Test
    void configuredFeaturesFiltered() {
        when(subscriptionMapper.selectOne(any())).thenReturn(sub(1L));
        when(planFeatureMapper.selectList(any())).thenReturn(List.of(
                feature(FeatureCodes.OKR, 1),
                feature(FeatureCodes.DOC, 1),
                feature(FeatureCodes.GANTT, 0)));
        List<String> enabled = service.enabledFeaturesForTenant(1L);
        assertTrue(enabled.contains(FeatureCodes.OKR));
        assertTrue(enabled.contains(FeatureCodes.DOC));
        assertFalse(enabled.contains(FeatureCodes.GANTT));
    }

    @Test
    void featuresOfPlanListsAllWithFlags() {
        when(planFeatureMapper.selectList(any())).thenReturn(List.of(feature(FeatureCodes.GANTT, 1)));
        var vos = service.featuresOfPlan(3L);
        assertEquals(FeatureCodes.ALL.size(), vos.size());
        assertTrue(vos.stream().anyMatch(v -> v.featureCode().equals(FeatureCodes.GANTT) && v.enabled()));
        assertTrue(vos.stream().anyMatch(v -> v.featureCode().equals(FeatureCodes.OKR) && !v.enabled()));
    }
}
