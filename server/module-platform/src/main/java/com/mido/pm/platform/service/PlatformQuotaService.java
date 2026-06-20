package com.mido.pm.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mido.pm.platform.entity.SysPlanQuota;
import com.mido.pm.platform.entity.SysTenantSubscription;
import com.mido.pm.platform.mapper.SysPlanQuotaMapper;
import com.mido.pm.platform.mapper.SysTenantSubscriptionMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 配额解析：按租户当前生效订阅，计算各资源的有效上限（套餐配额，叠加订阅级 quota_override 覆盖）。
 * 上限 -1 表示不限；无生效订阅时返回空（视为不限制）。
 */
@Service
public class PlatformQuotaService {

    /** 无限制哨兵 */
    public static final long UNLIMITED = -1L;

    private final SysTenantSubscriptionMapper subscriptionMapper;
    private final SysPlanQuotaMapper quotaMapper;
    private final ObjectMapper objectMapper;

    public PlatformQuotaService(SysTenantSubscriptionMapper subscriptionMapper,
                                SysPlanQuotaMapper quotaMapper, ObjectMapper objectMapper) {
        this.subscriptionMapper = subscriptionMapper;
        this.quotaMapper = quotaMapper;
        this.objectMapper = objectMapper;
    }

    /** 租户各资源有效上限（resource → limit）；无订阅返回空 map。 */
    public Map<String, Long> effectiveLimits(Long tenantId) {
        SysTenantSubscription sub = activeSubscription(tenantId);
        if (sub == null) {
            return Map.of();
        }
        Map<String, Long> limits = new HashMap<>();
        quotaMapper.selectList(Wrappers.<SysPlanQuota>lambdaQuery().eq(SysPlanQuota::getPlanId, sub.getPlanId()))
                .forEach(q -> limits.put(q.getResource(), q.getLimitValue()));
        // 订阅级覆盖（JSON: {resource: limit}）优先
        if (StringUtils.hasText(sub.getQuotaOverride())) {
            try {
                Map<String, Long> override = objectMapper.readValue(
                        sub.getQuotaOverride(), new TypeReference<Map<String, Long>>() {});
                limits.putAll(override);
            } catch (Exception ignore) {
                // 覆盖串异常时退回套餐默认，不阻断
            }
        }
        return limits;
    }

    /** 某资源有效上限；无订阅或未配置返回 UNLIMITED。 */
    public long effectiveLimit(Long tenantId, String resource) {
        return effectiveLimits(tenantId).getOrDefault(resource, UNLIMITED);
    }

    private SysTenantSubscription activeSubscription(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        return subscriptionMapper.selectOne(Wrappers.<SysTenantSubscription>lambdaQuery()
                .eq(SysTenantSubscription::getTenantId, tenantId)
                .eq(SysTenantSubscription::getStatus, "active")
                .orderByDesc(SysTenantSubscription::getId)
                .last("limit 1"));
    }
}
