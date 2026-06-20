package com.mido.pm.org.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录：username 为登录账号，可填手机号或用户名（双登录，租户内唯一）。
 * tenantCode 为租户编码，可空——缺省回落自用租户（阶段一对齐固定 tenant_id=1）。
 */
public record LoginDTO(
        @NotBlank(message = "账号不能为空") String username,
        @NotBlank(message = "密码不能为空") String password,
        String tenantCode) {
}
