package com.mido.pm.project.dto;

public record TemplateVO(
        Long id,
        String name,
        String category,
        String subCategory,
        String description,
        Integer isBuiltin) {
}
