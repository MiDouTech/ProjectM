package com.mido.pm.provider.sso;

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
 * 本地 SSO 实现：基于 JWT(jjwt) 签发与校验令牌。企微 SSO 实现（WecomSsoProvider）预留。
 */
@Component
public class LocalSsoProvider implements SsoProvider {

    private final SecretKey key;
    private final long ttlMillis;

    public LocalSsoProvider(
            @Value("${mido.jwt.secret:mido-pm-default-secret-key-please-change-in-prod}") String secret,
            @Value("${mido.jwt.ttl-millis:86400000}") long ttlMillis) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = ttlMillis;
    }

    @Override
    public String issueToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(key)
                .compact();
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
}
