package com.mido.pm.project.dto;

public record ProjectQueryDTO(
        Long page,
        Long size,
        String category,
        String status,
        Long leaderId,
        String keyword,
        // 归档过滤：null/0=仅在档（默认隐藏已归档），1=仅已归档
        Integer archived) {
}
