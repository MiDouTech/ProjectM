package com.mido.pm.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 重置平台账号密码。 */
public record ResetPasswordDTO(
        @NotBlank(message = "新密码不能为空")
        @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE) String password) {
}
