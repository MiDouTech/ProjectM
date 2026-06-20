package com.mido.pm.platform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 套餐功能开关保存入参（整体覆盖该套餐的功能开关集合）。 */
public record PlanFeatureDTO(@Valid List<Toggle> features) {

    /** 单个功能开关。 */
    public record Toggle(
            @NotBlank(message = "功能码不能为空") String featureCode,
            @NotNull(message = "启用状态不能为空") Boolean enabled) {
    }
}
