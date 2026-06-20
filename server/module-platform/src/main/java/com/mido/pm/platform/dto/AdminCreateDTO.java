package com.mido.pm.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 新建平台运营账号。 */
public record AdminCreateDTO(
        @NotBlank(message = "登录名不能为空") String username,
        @NotBlank(message = "姓名不能为空") String name,
        @NotBlank(message = "密码不能为空") String password,
        @NotEmpty(message = "至少分配一个角色") List<Long> roleIds) {
}
