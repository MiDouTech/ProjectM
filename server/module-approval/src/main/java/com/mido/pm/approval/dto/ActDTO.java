package com.mido.pm.approval.dto;

import jakarta.validation.constraints.NotBlank;

/** 审批动作。action：approve / reject。 */
public record ActDTO(
        @NotBlank(message = "审批动作不能为空") String action,
        String comment) {
}
