package com.mido.pm.platform.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 线下收入台账记录入参。type：payment 收款 / refund 退款。金额一律为正，方向由 type 表达。 */
public record RevenueRecordDTO(
        @NotNull(message = "租户不能为空") Long tenantId,
        @NotBlank(message = "类型不能为空") String type,
        @NotNull(message = "金额不能为空")
        @DecimalMin(value = "0.01", message = "金额必须大于0") BigDecimal amount,
        String currency,
        Long subscriptionId,
        String contractNo,
        LocalDate occurredDate,
        String remark) {
}
