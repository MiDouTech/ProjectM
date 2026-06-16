package com.mido.pm.provider.sso;

/**
 * 企微 SSO Provider（TODO·P2 激活）：OAuth2 / SAML2.0 登录。
 * 阶段一不注册为 Bean，占位预留。
 */
public class WecomSsoProvider implements SsoProvider {

    @Override
    public String login(String username, String rawPassword) {
        throw new UnsupportedOperationException("TODO: 企微 OAuth2/SAML 登录，P2 激活");
    }

    @Override
    public Long verifyToken(String token) {
        throw new UnsupportedOperationException("TODO: 企微 SSO 令牌校验，P2 激活");
    }

    @Override
    public String refreshToken(String token) {
        throw new UnsupportedOperationException("TODO: 企微 SSO 令牌刷新，P2 激活");
    }
}
