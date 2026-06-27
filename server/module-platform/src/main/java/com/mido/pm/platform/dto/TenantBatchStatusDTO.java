package com.mido.pm.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 租户批量状态流转入参。 */
public record TenantBatchStatusDTO(
        @NotEmpty(message = "请至少选择一个租户") List<Long> ids,
        @NotBlank(message = "目标状态不能为空") String status,
        String reason) {
}
