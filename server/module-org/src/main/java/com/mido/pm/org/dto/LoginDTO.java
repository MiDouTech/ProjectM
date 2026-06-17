package com.mido.pm.org.dto;

import jakarta.validation.constraints.NotBlank;

/** 登录：username 为登录账号，可填手机号或用户名（双登录）。 */
public record LoginDTO(
        @NotBlank(message = "账号不能为空") String username,
        @NotBlank(message = "密码不能为空") String password) {
}
