package com.mido.pm.approval.dto;

import jakarta.validation.constraints.NotNull;

/** 转交审批：toUserId 受让人（必填），comment 转交说明（选填）。 */
public record TransferDTO(
        @NotNull(message = "转交对象不能为空") Long toUserId,
        String comment) {
}
