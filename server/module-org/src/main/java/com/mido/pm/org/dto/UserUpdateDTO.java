package com.mido.pm.org.dto;

import jakarta.validation.constraints.NotBlank;

public record UserUpdateDTO(
        @NotBlank(message = "姓名不能为空") String name,
        Long deptId,
        String jobLevel,
        String avatar,
        String status) {
}
