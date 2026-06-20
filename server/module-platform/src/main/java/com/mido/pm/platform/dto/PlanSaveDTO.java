package com.mido.pm.platform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

/** 套餐保存入参（含配额项整体覆盖）。code 仅新建时使用，编辑忽略。 */
public record PlanSaveDTO(
        @NotBlank(message = "套餐编码不能为空") String code,
        @NotBlank(message = "套餐名称不能为空") String name,
        BigDecimal price,
        String billingCycle,
        String status,
        Integer sort,
        String remark,
        @Valid List<QuotaDTO> quotas) {
}
