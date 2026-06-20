package com.mido.pm.platform.dto;

import jakarta.validation.constraints.NotBlank;

/** 重置平台账号密码。 */
public record ResetPasswordDTO(
        @NotBlank(message = "新密码不能为空") String password) {
}
