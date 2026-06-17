package com.mido.pm.approval.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/** 提交审批。formData 同时作为条件路由/guard 的上下文(含 amount/category/jobLevel 等)。 */
public record SubmitDTO(
        @NotNull(message = "审批流不能为空") Long flowId,
        String bizType,
        Long bizId,
        Map<String, Object> formData) {
}
