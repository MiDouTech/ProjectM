package com.mido.pm.org.dto;

import jakarta.validation.constraints.NotBlank;

public record UserCreateDTO(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "姓名不能为空") String name,
        @NotBlank(message = "密码不能为空") String password,
        Long deptId,
        String jobLevel,
        String status) {
}
