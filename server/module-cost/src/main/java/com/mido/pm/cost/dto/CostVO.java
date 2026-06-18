package com.mido.pm.cost.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 费用视图。 */
public record CostVO(
        Long id,
        Long projectId,
        String title,
        String account,
        BigDecimal budgetAmount,
        BigDecimal actualAmount,
        LocalDate occurDate,
        LocalDate payDate,
        String status,
        Long approvalId,
        LocalDateTime createTime) {
}
