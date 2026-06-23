package com.mido.pm.briefing.dto;

import jakarta.validation.constraints.NotBlank;

/** 评审批注入参。action 缺省 comment（comment 批注 / approve 已阅）。 */
public record BriefingReviewDTO(
        @NotBlank(message = "批注内容不能为空") String comment,
        String action) {
}
