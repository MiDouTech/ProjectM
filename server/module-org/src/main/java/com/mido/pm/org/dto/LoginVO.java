package com.mido.pm.org.dto;

/** 登录返回：访问令牌 + 类型 + 有效期(秒)。 */
public record LoginVO(String token, String tokenType, long expiresIn) {
}
