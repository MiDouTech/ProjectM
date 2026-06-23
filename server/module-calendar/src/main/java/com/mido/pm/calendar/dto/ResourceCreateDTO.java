package com.mido.pm.calendar.dto;

import jakarta.validation.constraints.NotBlank;

/** 新建资源。type 缺省 room。 */
public record ResourceCreateDTO(
        @NotBlank(message = "资源名称不能为空") String name,
        String type,
        Integer capacity,
        String location) {
}
