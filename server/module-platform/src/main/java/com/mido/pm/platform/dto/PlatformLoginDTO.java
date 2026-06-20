package com.mido.pm.platform.dto;

import jakarta.validation.constraints.NotBlank;

/** 平台运营登录入参。 */
public record PlatformLoginDTO(
        @NotBlank(message = "账号不能为空") String username,
        @NotBlank(message = "密码不能为空") String password) {
}
