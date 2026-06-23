package com.mido.pm.org.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.org.dto.LoginDTO;
import com.mido.pm.org.dto.LoginVO;
import com.mido.pm.provider.sso.SsoProvider;
import com.mido.pm.provider.sso.WecomSsoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口：账号密码登录、令牌刷新、企微 SSO。业务层只依赖 {@link SsoProvider}/{@link WecomSsoService}，
 * 不写企微细节。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String BEARER = "Bearer ";

    private final SsoProvider ssoProvider;
    private final WecomSsoService wecomSsoService;
    private final long ttlMillis;

    public AuthController(SsoProvider ssoProvider, WecomSsoService wecomSsoService,
                          @Value("${mido.jwt.ttl-millis:86400000}") long ttlMillis) {
        this.ssoProvider = ssoProvider;
        this.wecomSsoService = wecomSsoService;
        this.ttlMillis = ttlMillis;
    }

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        String token = ssoProvider.login(dto.username(), dto.password(), dto.tenantCode());
        return R.ok(new LoginVO(token, "Bearer", ttlMillis / 1000));
    }

    @PostMapping("/refresh")
    public R<LoginVO> refresh(@RequestHeader("Authorization") String authorization) {
        String token = authorization != null && authorization.startsWith(BEARER)
                ? authorization.substring(BEARER.length()) : authorization;
        String fresh = ssoProvider.refreshToken(token);
        if (fresh == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "令牌无效或已过期");
        }
        return R.ok(new LoginVO(fresh, "Bearer", ttlMillis / 1000));
    }

    /** 企微 SSO 授权地址：前端据 enabled 决定是否展示「企微登录」，redirectUri 为前端回调页。 */
    @GetMapping("/wecom/authorize-url")
    public R<Map<String, Object>> wecomAuthorizeUrl(@RequestParam String redirectUri,
                                                    @RequestParam(required = false) String state) {
        boolean enabled = wecomSsoService.enabled();
        String url = enabled ? wecomSsoService.authorizeUrl(redirectUri, state) : "";
        return R.ok(Map.of("enabled", enabled, "url", url));
    }

    /** 企微 SSO 登录：用授权 code 换访问令牌。 */
    @PostMapping("/wecom/login")
    public R<LoginVO> wecomLogin(@RequestBody Map<String, String> body) {
        String token = wecomSsoService.loginByCode(body.get("code"));
        return R.ok(new LoginVO(token, "Bearer", ttlMillis / 1000));
    }
}
