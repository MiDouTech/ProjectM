package com.mido.pm.org.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleUpdateDTO(
        @NotBlank(message = "角色名不能为空") String name,
        @NotBlank(message = "角色编码不能为空") String code) {
}
