package com.mido.pm.briefing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** 提出跟进问题。owner 缺省为提出人本人。 */
public record IssueCreateDTO(
        @NotNull(message = "来源简报不能为空") Long briefingId,
        @NotBlank(message = "问题内容不能为空") String content,
        Long ownerId,
        LocalDate dueDate) {
}
