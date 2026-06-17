package com.mido.pm.provider.sso;

/**
 * 单点登录 Provider 接口：账号密码登录换取 JWT、校验、刷新。
 * 本地实现 {@code LocalSsoProvider}；企微 SSO（OAuth2/SAML）实现 {@code WecomSsoProvider} 预留。
 */
public interface SsoProvider {

    /**
     * 账号密码登录，成功返回访问令牌（JWT）。
     *
     * @throws com.mido.pm.common.exception.BizException 凭证无效或账号停用
     */
    String login(String username, String rawPassword);

    /** 校验令牌并返回用户 ID；无效返回 null。 */
    Long verifyToken(String token);

    /** 用仍有效的令牌换取新令牌（刷新）；无效返回 null。 */
    String refreshToken(String token);
}
