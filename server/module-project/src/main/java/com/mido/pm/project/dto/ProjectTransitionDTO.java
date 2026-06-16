package com.mido.pm.project.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 状态流转请求。targetStatus 为目标状态中文 code；
 * approvalPassed 为审批结果钩子（→已注册时由审批流 Step 3 传 true；为 false 则拒绝）。
 */
public record ProjectTransitionDTO(
        @NotBlank(message = "目标状态不能为空") String targetStatus,
        Boolean approvalPassed) {
}
