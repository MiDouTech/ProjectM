package com.mido.pm.project.dto;

import jakarta.validation.constraints.NotBlank;

/** 项目集创建/更新入参。 */
public record PortfolioSaveDTO(
        @NotBlank(message = "项目集名称不能为空") String name,
        String description,
        Long ownerId,
        String status) {
}
