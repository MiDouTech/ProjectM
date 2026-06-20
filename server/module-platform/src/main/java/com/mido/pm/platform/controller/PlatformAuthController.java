package com.mido.pm.platform.controller;

import com.mido.pm.common.api.R;
import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.platform.dto.PlatformLoginDTO;
import com.mido.pm.platform.dto.PlatformLoginVO;
import com.mido.pm.platform.dto.PlatformMeVO;
import com.mido.pm.platform.security.PlatformContext;
import com.mido.pm.platform.security.PlatformPrincipal;
import com.mido.pm.platform.security.PlatformTokenService;
import com.mido.pm.platform.service.PlatformAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台运营认证：账号密码登录（独立令牌）、当前账号信息。
 * 登录端点在安全链放行，其余 /api/v1/platform/** 需平台令牌。
 */
@RestController
@RequestMapping("/api/v1/platform/auth")
public class PlatformAuthController {

    private final PlatformAuthService authService;
    private final PlatformTokenService tokenService;

    public PlatformAuthController(PlatformAuthService authService, PlatformTokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public R<PlatformLoginVO> login(@Valid @RequestBody PlatformLoginDTO dto) {
        Long adminId = authService.login(dto.username(), dto.password());
        String token = tokenService.issue(adminId);
        return R.ok(new PlatformLoginVO(token, "Bearer", tokenService.ttlSeconds()));
    }

    @GetMapping("/me")
    public R<PlatformMeVO> me() {
        PlatformPrincipal p = PlatformContext.get();
        if (p == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未认证或登录已过期");
        }
        return R.ok(new PlatformMeVO(p.adminId(), p.username(), p.name(), p.permCodes()));
    }
}
