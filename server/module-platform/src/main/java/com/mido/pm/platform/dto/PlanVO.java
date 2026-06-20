package com.mido.pm.platform.dto;

import java.math.BigDecimal;
import java.util.List;

/** 套餐视图（含配额项）。 */
public record PlanVO(
        Long id,
        String code,
        String name,
        BigDecimal price,
        String billingCycle,
        String status,
        Integer sort,
        String remark,
        List<QuotaVO> quotas) {
}
