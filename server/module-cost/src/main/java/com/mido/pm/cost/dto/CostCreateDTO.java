package com.mido.pm.cost.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 登记费用入参。status 不传默认未发生；actual 可空（发生后再填）。 */
public record CostCreateDTO(
        @NotNull(message = "项目不能为空") Long projectId,
        @NotBlank(message = "费用标题不能为空") String title,
        @NotBlank(message = "费用科目不能为空") String account,
        @NotNull(message = "预算金额不能为空") @DecimalMin(value = "0.00", message = "预算金额不能为负") BigDecimal budgetAmount,
        @DecimalMin(value = "0.00", message = "实际金额不能为负") BigDecimal actualAmount,
        LocalDate occurDate,
        LocalDate payDate) {
}
