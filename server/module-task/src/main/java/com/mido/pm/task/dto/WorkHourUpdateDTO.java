package com.mido.pm.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 修改工时入参（kind 不可改）。 */
public record WorkHourUpdateDTO(
        @NotBlank(message = "工时类别不能为空") String category,
        @NotNull(message = "工时日期不能为空") LocalDate workDate,
        @NotNull(message = "工时不能为空") @Positive(message = "工时须大于 0") BigDecimal hours,
        String remark) {
}
