package com.mido.pm.org.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.org.dto.LoginDTO;
import com.mido.pm.org.dto.LoginVO;
import com.mido.pm.provider.sso.SsoProvider;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：账号密码登录、令牌刷新。业务层只依赖 {@link SsoProvider}，不写企微逻辑。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String BEARER = "Bearer ";

    private final SsoProvider ssoProvider;
    private final long ttlMillis;

    public AuthController(SsoProvider ssoProvider,
                          @Value("${mido.jwt.ttl-millis:86400000}") long ttlMillis) {
        this.ssoProvider = ssoProvider;
        this.ttlMillis = ttlMillis;
    }

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        String token = ssoProvider.login(dto.username(), dto.password());
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
}
