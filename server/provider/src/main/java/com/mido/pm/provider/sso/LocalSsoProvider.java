package com.mido.pm.provider.sso;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.provider.identity.IdentityProvider;
import com.mido.pm.provider.identity.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 本地 SSO 实现：账号密码登录、签发/校验/刷新 JWT（jjwt）。
 * 凭证经 {@link IdentityProvider} 装配、{@link PasswordEncoder} 校验。
 * 企微 SSO 实现 {@code WecomSsoProvider} 预留。
 */
@Component
public class LocalSsoProvider implements SsoProvider {

    private static final String STATUS_DISABLED = "disabled";

    private final SecretKey key;
    private final long ttlMillis;
    private final IdentityProvider identityProvider;
    private final PasswordEncoder passwordEncoder;

    public LocalSsoProvider(
            @Value("${mido.jwt.secret:mido-pm-default-secret-key-please-change-in-prod}") String secret,
            @Value("${mido.jwt.ttl-millis:86400000}") long ttlMillis,
            IdentityProvider identityProvider,
            PasswordEncoder passwordEncoder) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        // HS256 要求密钥至少 256bit(32 字节)。提前显式校验，给出清晰提示。
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "mido.jwt.secret 长度不足：HS256 需至少 32 字节(256bit)，当前 "
                            + secretBytes.length + " 字节。请将 MIDO_JWT_SECRET 设为不少于 32 个字符。");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.ttlMillis = ttlMillis;
        this.identityProvider = identityProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String login(String username, String rawPassword) {
        UserPrincipal principal = identityProvider.loadByUsername(username)
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误"));
        if (!passwordEncoder.matches(rawPassword, principal.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (STATUS_DISABLED.equalsIgnoreCase(principal.getStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已停用");
        }
        return issueToken(principal.getUserId());
    }

    @Override
    public Long verifyToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (JwtException | NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String refreshToken(String token) {
        Long userId = verifyToken(token);
        return userId == null ? null : issueToken(userId);
    }

    private String issueToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(key)
                .compact();
    }
}
