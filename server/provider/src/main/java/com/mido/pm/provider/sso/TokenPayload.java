package com.mido.pm.provider.sso;

/**
 * 令牌载荷：校验通过后解出的身份信息。
 *
 * @param userId         用户 ID（subject）
 * @param tenantId       所属租户 ID（多租户隔离据此注入）
 * @param impersonatedBy 模拟登录的平台运营账号 ID；非模拟登录为 null
 */
public record TokenPayload(Long userId, Long tenantId, Long impersonatedBy) {
}
