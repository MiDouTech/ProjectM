package com.mido.pm.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 租户详情：基础信息 + 当前订阅 + 生效配额项。 */
public record TenantDetailVO(
        Long id,
        String code,
        String name,
        String status,
        String industry,
        String contactName,
        String contactPhone,
        String contactEmail,
        String source,
        String remark,
        LocalDateTime activatedAt,
        LocalDateTime expireAt,
        LocalDateTime createTime,
        SubscriptionVO subscription,
        List<QuotaVO> quotas) {
}
