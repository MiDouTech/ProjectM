package com.mido.pm.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 平台账号 JWT 签发/校验。使用<strong>独立密钥</strong>（{@code mido.platform.jwt.secret}），
 * 与租户侧 SSO（{@code LocalSsoProvider}）物理隔离：租户令牌无法被平台校验通过，反之亦然。
 * 令牌额外携带 {@code typ=platform} 声明做二次甄别。
 */
@Component
public class PlatformTokenService {

    private static final String TYP_CLAIM = "typ";
    private static final String TYP_PLATFORM = "platform";

    private final SecretKey key;
    private final long ttlMillis;

    public PlatformTokenService(
            @Value("${mido.platform.jwt.secret:mido-platform-console-secret-key-change-me}") String secret,
            @Value("${mido.platform.jwt.ttl-millis:43200000}") long ttlMillis) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "mido.platform.jwt.secret 长度不足：HS256 需至少 32 字节(256bit)，当前 "
                            + secretBytes.length + " 字节。请将 MIDO_PLATFORM_JWT_SECRET 设为不少于 32 个字符。");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.ttlMillis = ttlMillis;
    }

    /** 签发平台账号访问令牌。subject=平台账号 ID。 */
    public String issue(Long adminId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .claim(TYP_CLAIM, TYP_PLATFORM)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(key)
                .compact();
    }

    /** 校验令牌并返回平台账号 ID；无效或非平台令牌返回 null。 */
    public Long verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!TYP_PLATFORM.equals(claims.get(TYP_CLAIM, String.class))) {
                return null;
            }
            return Long.valueOf(claims.getSubject());
        } catch (JwtException | NumberFormatException e) {
            return null;
        }
    }

    /** 令牌有效期（秒），供登录响应回传。 */
    public long ttlSeconds() {
        return ttlMillis / 1000;
    }
}
