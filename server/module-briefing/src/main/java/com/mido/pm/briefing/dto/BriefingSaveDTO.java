package com.mido.pm.briefing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Map;

/**
 * 保存简报草稿（按 模板+周期 幂等 upsert 到当前用户）。
 * content 为各字段填写内容（key→文本）。
 */
public record BriefingSaveDTO(
        @NotNull(message = "模板不能为空") Long templateId,
        @NotBlank(message = "周期标识不能为空") String periodKey,
        LocalDate periodStart,
        LocalDate periodEnd,
        Map<String, Object> content) {
}
