package com.mido.pm.task.dto;

import jakarta.validation.constraints.NotBlank;

/** 状态流转（看板拖拽亦走此）。 */
public record TaskTransitionDTO(@NotBlank(message = "目标状态不能为空") String targetStatus) {
}
