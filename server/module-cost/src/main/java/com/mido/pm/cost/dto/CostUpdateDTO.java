package com.mido.pm.cost.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 修改费用入参（status 由审批结果驱动，此处不改）。 */
public record CostUpdateDTO(
        @NotBlank(message = "费用标题不能为空") String title,
        @NotBlank(message = "费用科目不能为空") String account,
        @NotNull(message = "预算金额不能为空") @DecimalMin(value = "0.00", message = "预算金额不能为负") BigDecimal budgetAmount,
        @DecimalMin(value = "0.00", message = "实际金额不能为负") BigDecimal actualAmount,
        LocalDate occurDate,
        LocalDate payDate) {
}
