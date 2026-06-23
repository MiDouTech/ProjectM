package com.mido.pm.briefing.dto;

import java.time.LocalDateTime;

/** 评审批注视图。 */
public record BriefingReviewVO(
        Long id,
        Long reviewerId,
        String action,
        String comment,
        LocalDateTime reviewedAt) {
}
