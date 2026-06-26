package com.mido.pm.report.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 报表设置更新：财年起始月 1-12。 */
public record ReportSettingUpdateDTO(
        @NotNull(message = "财年起始月不能为空")
        @Min(value = 1, message = "财年起始月须在 1-12 之间")
        @Max(value = 12, message = "财年起始月须在 1-12 之间")
        Integer fiscalYearStartMonth) {
}
