package com.mido.pm.verify.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 干系人提交评分：0-10 分 + comment 必填（npss-rule §3.4 禁人情分，鼓励真实反馈）。 */
public record ScoreSubmitDTO(
        @NotNull(message = "干系人不能为空") Long stakeholderId,
        @NotNull(message = "评分不能为空") @Min(value = 0, message = "评分 0-10") @Max(value = 10, message = "评分 0-10") Integer score,
        @NotBlank(message = "请填写评价理由（必填，禁人情分）") String comment) {
}
