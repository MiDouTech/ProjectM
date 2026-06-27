package com.mido.pm.platform.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 线下收入台账视图。 */
public record RevenueVO(
        Long id,
        Long tenantId,
        String tenantName,
        Long subscriptionId,
        String type,
        BigDecimal amount,
        String currency,
        String contractNo,
        LocalDate occurredDate,
        String remark,
        LocalDateTime createTime) {
}
