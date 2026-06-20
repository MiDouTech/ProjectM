package com.mido.pm.platform.dto;

import java.time.LocalDateTime;

/** 租户当前订阅视图。 */
public record SubscriptionVO(
        Long id,
        Long planId,
        String planName,
        String status,
        LocalDateTime startAt,
        LocalDateTime expireAt,
        String remark) {
}
