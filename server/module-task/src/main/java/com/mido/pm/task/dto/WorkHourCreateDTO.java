package com.mido.pm.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 登记工时入参。kind：est/actual；category：设计/研发/文档/测试/其他（服务端校验）。 */
public record WorkHourCreateDTO(
        @NotNull(message = "任务不能为空") Long taskId,
        @NotBlank(message = "工时类型不能为空") String kind,
        @NotBlank(message = "工时类别不能为空") String category,
        @NotNull(message = "工时日期不能为空") LocalDate workDate,
        @NotNull(message = "工时不能为空") @Positive(message = "工时须大于 0") BigDecimal hours,
        String remark) {
}
