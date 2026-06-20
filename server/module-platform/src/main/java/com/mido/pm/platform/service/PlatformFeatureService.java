package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mido.pm.common.feature.FeatureCodes;
import com.mido.pm.platform.dto.PlanFeatureDTO;
import com.mido.pm.platform.dto.PlanFeatureVO;
import com.mido.pm.platform.entity.SysPlanFeature;
import com.mido.pm.platform.entity.SysTenantSubscription;
import com.mido.pm.platform.mapper.SysPlanFeatureMapper;
import com.mido.pm.platform.mapper.SysTenantSubscriptionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 功能开关按套餐下发：运营配置 plan→feature；租户据生效套餐查询启用功能码（前端门控）。
 * 容错：某套餐未配置任何功能开关时视为全部启用（不因未配置而误关功能）。
 */
@Service
public class PlatformFeatureService {

    private final SysPlanFeatureMapper planFeatureMapper;
    private final SysTenantSubscriptionMapper subscriptionMapper;
    private final PlatformAuditService auditService;

    public PlatformFeatureService(SysPlanFeatureMapper planFeatureMapper,
                                  SysTenantSubscriptionMapper subscriptionMapper,
                                  PlatformAuditService auditService) {
        this.planFeatureMapper = planFeatureMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.auditService = auditService;
    }

    /** 某套餐的功能开关全集（按 FeatureCodes.ALL 列全，未配置视为关）。 */
    public List<PlanFeatureVO> featuresOfPlan(Long planId) {
        Map<String, Integer> configured = planFeatureMapper.selectList(Wrappers.<SysPlanFeature>lambdaQuery()
                        .eq(SysPlanFeature::getPlanId, planId))
                .stream().collect(Collectors.toMap(SysPlanFeature::getFeatureCode,
                        f -> f.getEnabled() == null ? 0 : f.getEnabled(), (a, b) -> b));
        return FeatureCodes.ALL.stream()
                .map(code -> new PlanFeatureVO(code, Integer.valueOf(1).equals(configured.get(code))))
                .toList();
    }

    /** 整体覆盖某套餐的功能开关集合。 */
    @Transactional(rollbackFor = Exception.class)
    public void saveFeatures(Long planId, PlanFeatureDTO dto) {
        planFeatureMapper.delete(Wrappers.<SysPlanFeature>lambdaQuery().eq(SysPlanFeature::getPlanId, planId));
        if (dto.features() != null) {
            for (PlanFeatureDTO.Toggle t : dto.features()) {
                SysPlanFeature f = new SysPlanFeature();
                f.setPlanId(planId);
                f.setFeatureCode(t.featureCode());
                f.setEnabled(Boolean.TRUE.equals(t.enabled()) ? 1 : 0);
                planFeatureMapper.insert(f);
            }
        }
        auditService.record(PlatformAuditActions.PLAN_FEATURE_SAVED, PlatformAuditActions.TARGET_PLAN, planId, null);
    }

    /** 租户据其生效套餐返回启用的功能码集合（未订阅或套餐未配置→全部启用）。 */
    public List<String> enabledFeaturesForTenant(Long tenantId) {
        SysTenantSubscription sub = subscriptionMapper.selectOne(Wrappers.<SysTenantSubscription>lambdaQuery()
                .eq(SysTenantSubscription::getTenantId, tenantId)
                .eq(SysTenantSubscription::getStatus, "active")
                .orderByDesc(SysTenantSubscription::getId).last("limit 1"));
        if (sub == null) {
            return FeatureCodes.ALL;
        }
        List<SysPlanFeature> rows = planFeatureMapper.selectList(Wrappers.<SysPlanFeature>lambdaQuery()
                .eq(SysPlanFeature::getPlanId, sub.getPlanId()));
        if (rows.isEmpty()) {
            return FeatureCodes.ALL;
        }
        Set<String> enabled = rows.stream()
                .filter(f -> Integer.valueOf(1).equals(f.getEnabled()))
                .map(SysPlanFeature::getFeatureCode).collect(Collectors.toSet());
        return FeatureCodes.ALL.stream().filter(enabled::contains).toList();
    }
}
