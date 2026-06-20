package com.mido.pm.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 配额项入参。limitValue=-1 表示不限。 */
public record QuotaDTO(
        @NotBlank(message = "配额资源不能为空") String resource,
        @NotNull(message = "配额上限不能为空") Long limitValue) {
}
