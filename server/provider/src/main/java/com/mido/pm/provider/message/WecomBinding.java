package com.mido.pm.provider.message;

/**
 * 企微账号绑定：企微 userid → 本地账号（含租户）。SSO 无租户上下文，故携带 tenantId 以便签发令牌。
 */
public record WecomBinding(Long userId, Long tenantId) {
}
