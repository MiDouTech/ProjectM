package com.mido.pm.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员重置用户密码：直接设置新密码（不校验原密码）。新密码长度 8-64。
 */
public record ResetPasswordDTO(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 64, message = "新密码长度需为 8-64 位") String newPassword) {
}
