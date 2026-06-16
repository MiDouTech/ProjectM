package com.mido.pm.provider.sso;

/**
 * 单点登录 Provider 接口。负责令牌签发与校验（本地 JWT 或企微 SSO）。
 */
public interface SsoProvider {

    /** 为用户签发访问令牌。 */
    String issueToken(Long userId);

    /** 校验令牌并返回用户 ID；无效返回 null。 */
    Long verifyToken(String token);
}
