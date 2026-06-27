package com.mido.pm.project.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** 项目集创建/更新入参。memberIds 为成员用户 id（replace-all；null 表示不改成员，创建时自动含创建人）。 */
public record PortfolioSaveDTO(
        @NotBlank(message = "项目集名称不能为空") String name,
        String description,
        Long ownerId,
        String status,
        List<Long> memberIds) {
}
