package com.mido.pm.stakeholder.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WeightItemDTO(
        @NotNull(message = "干系人不能为空") Long stakeholderId,
        @NotNull(message = "权重不能为空") BigDecimal npssWeight) {
}
