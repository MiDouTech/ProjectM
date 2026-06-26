package com.mido.pm.task.dto;

/** 优先级档位项。levelValue 越小优先级越高。 */
public record PriorityLevelDTO(Long id, String name, String color, Integer levelValue, Integer sort) {
}
