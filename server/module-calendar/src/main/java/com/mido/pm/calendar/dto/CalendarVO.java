package com.mido.pm.calendar.dto;

/** 日历容器视图。 */
public record CalendarVO(
        Long id,
        String name,
        String type,
        Long ownerId,
        String color,
        String visibility,
        Integer isDefault,
        String status) {
}
