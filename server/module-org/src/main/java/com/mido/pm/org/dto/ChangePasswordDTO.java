package com.mido.pm.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 自助修改密码：校验原密码后设置新密码。新密码长度 8-64。
 */
public record ChangePasswordDTO(
        @NotBlank(message = "原密码不能为空") String oldPassword,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 64, message = "新密码长度需为 8-64 位") String newPassword) {
}
