package com.mido.pm.calendar.dto;

/** 日历资源视图。 */
public record ResourceVO(
        Long id,
        String name,
        String type,
        Integer capacity,
        String location,
        String status) {
}
