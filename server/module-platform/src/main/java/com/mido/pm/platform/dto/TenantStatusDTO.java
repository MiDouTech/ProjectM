package com.mido.pm.platform.dto;

import jakarta.validation.constraints.NotBlank;

/** 变更租户状态：active 启用 / suspended 停用 / closed 注销。 */
public record TenantStatusDTO(
        @NotBlank(message = "目标状态不能为空") String status,
        String reason) {
}
