package com.mido.pm.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 新建成员：以手机号为登录账号（全局唯一）。用户名可选，缺省取手机号。
 */
public record UserCreateDTO(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
        String username,
        @NotBlank(message = "姓名不能为空") String name,
        @NotBlank(message = "密码不能为空") String password,
        Long deptId,
        String jobLevel,
        String avatar,
        String status) {
}
