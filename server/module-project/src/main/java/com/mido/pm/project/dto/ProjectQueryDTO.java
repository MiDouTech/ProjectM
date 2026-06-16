package com.mido.pm.project.dto;

public record ProjectQueryDTO(
        Long page,
        Long size,
        String category,
        String status,
        Long leaderId,
        String keyword) {
}
