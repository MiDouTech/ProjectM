package com.mido.pm.platform.dto;

/** 平台运营登录返回：令牌 + 类型 + 有效期(秒) + 是否需强制改密。 */
public record PlatformLoginVO(String token, String tokenType, long expiresIn, boolean mustChangePassword) {
}
