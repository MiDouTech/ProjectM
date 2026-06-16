package com.mido.pm.stakeholder.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 保存/微调项目干系人权重（保存时做 npss-rule §4 硬校验）。 */
public record SaveWeightsDTO(
        @NotNull(message = "项目不能为空") Long projectId,
        List<WeightItemDTO> items) {
}
