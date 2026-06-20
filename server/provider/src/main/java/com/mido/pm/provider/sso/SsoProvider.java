package com.mido.pm.provider.sso;

/**
 * 单点登录 Provider 接口：账号密码登录换取 JWT、校验、刷新、模拟登录签发。
 * 令牌携带租户声明（多租户隔离）。本地实现 {@code LocalSsoProvider}；企微 {@code WecomSsoProvider} 预留。
 */
public interface SsoProvider {

    /**
     * 账号密码登录，成功返回访问令牌（JWT，含租户声明）。
     *
     * @param account     手机号或用户名（租户内唯一）
     * @param rawPassword 明文密码
     * @param tenantCode  租户编码；为空回落自用租户（见 TenantDirectory.defaultTenantId）
     * @throws com.mido.pm.common.exception.BizException 凭证无效 / 账号停用 / 租户不可登录
     */
    String login(String account, String rawPassword, String tenantCode);

    /** 校验令牌并返回载荷（用户+租户+模拟来源）；无效返回 null。 */
    TokenPayload verifyToken(String token);

    /** 用仍有效的令牌换取新令牌（保留租户与模拟来源）；无效返回 null。 */
    String refreshToken(String token);

    /**
     * 签发模拟登录令牌（平台运营进租户排障用）：短时有效，携带 impersonatedBy 声明以便审计。
     *
     * @param userId               被模拟的租户用户 ID
     * @param tenantId             目标租户 ID
     * @param impersonatedByAdminId 发起模拟的平台运营账号 ID
     */
    String issueImpersonationToken(Long userId, Long tenantId, Long impersonatedByAdminId);
}
