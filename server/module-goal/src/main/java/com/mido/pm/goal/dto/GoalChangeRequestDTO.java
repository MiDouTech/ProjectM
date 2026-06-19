package com.mido.pm.goal.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * 发起目标变更。changeType 决定审批策略；reason 必填（留痕）。
 * 拟改值仅填需变更的字段（null=不改）；进度(current) 不属变更范畴。
 */
public record GoalChangeRequestDTO(
        @NotBlank(message = "变更类型不能为空") String changeType,
        @NotBlank(message = "变更事由不能为空") String reason,
        String impact,
        String title,
        Long ownerId,
        String period,
        String metricUnit,
        BigDecimal metricStart,
        BigDecimal metricTarget) {
}
