package com.mido.pm.provider.sso;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.provider.message.WecomBinding;
import com.mido.pm.provider.message.WecomUserResolver;
import org.springframework.stereotype.Service;

/**
 * 企微 SSO 编排：授权 URL 下发 + code 登录（换 userid → sys_identity_map 反查本地账号 → 签发 JWT）。
 * 与账号密码登录并行的外部身份登录通道；本地账号须先在 sys_identity_map 绑定企微 userid。
 */
@Service
public class WecomSsoService {

    private final WecomSsoClient ssoClient;
    private final WecomUserResolver userResolver;
    private final SsoProvider ssoProvider;

    public WecomSsoService(WecomSsoClient ssoClient, WecomUserResolver userResolver, SsoProvider ssoProvider) {
        this.ssoClient = ssoClient;
        this.userResolver = userResolver;
        this.ssoProvider = ssoProvider;
    }

    public boolean enabled() {
        return ssoClient.enabled();
    }

    /** 构建授权 URL（未启用则报错）。 */
    public String authorizeUrl(String redirectUri, String state) {
        requireEnabled();
        return ssoClient.authorizeUrl(redirectUri, state == null ? "" : state);
    }

    /** 用授权 code 登录，返回访问令牌。 */
    public String loginByCode(String code) {
        requireEnabled();
        if (code == null || code.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "缺少授权 code");
        }
        String wecomUserId = ssoClient.userIdByCode(code);
        if (wecomUserId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "企微授权失败");
        }
        WecomBinding binding = userResolver.bindingByExternalId(wecomUserId);
        if (binding == null) {
            throw new BizException(ErrorCode.FORBIDDEN, "该企微账号未绑定系统用户，请联系管理员");
        }
        return ssoProvider.issueAccessToken(binding.userId(), binding.tenantId());
    }

    private void requireEnabled() {
        if (!ssoClient.enabled()) {
            throw new BizException(ErrorCode.FORBIDDEN, "企微登录未启用");
        }
    }
}
