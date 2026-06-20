package com.mido.pm.platform.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 绑定/续期租户订阅入参。绑定后同步租户 expireAt 与状态（active）。
 * expireAt 为空表示不限期（如自用租户）。
 */
public record SubscriptionSaveDTO(
        @NotNull(message = "套餐不能为空") Long planId,
        LocalDateTime startAt,
        LocalDateTime expireAt,
        String remark) {
}
