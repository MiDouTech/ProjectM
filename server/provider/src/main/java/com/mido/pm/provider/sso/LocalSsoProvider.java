package com.mido.pm.provider.sso;

import com.mido.pm.common.exception.BizException;
import com.mido.pm.common.exception.ErrorCode;
import com.mido.pm.common.tenant.TenantContext;
import com.mido.pm.common.tenant.TenantDirectory;
import com.mido.pm.provider.identity.IdentityProvider;
import com.mido.pm.provider.identity.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 本地 SSO 实现：账号密码登录、签发/校验/刷新 JWT（jjwt），令牌携带租户声明（tid）与模拟来源（imp）。
 * 多租户登录隔离：登录先经 {@link TenantDirectory} 按租户编码解析并校验租户，设置 {@link TenantContext}
 * 后再做租户内身份查询（手机号/用户名租户内唯一）。企微 SSO 实现 {@code WecomSsoProvider} 预留。
 */
@Component
public class LocalSsoProvider implements SsoProvider {

    private static final String STATUS_DISABLED = "disabled";
    private static final String CLAIM_TENANT = "tid";
    private static final String CLAIM_IMPERSONATOR = "imp";
    /** 中国大陆 11 位手机号：用于判定登录账号走手机号还是用户名查询 */
    private static final java.util.regex.Pattern PHONE = java.util.regex.Pattern.compile("^1[3-9]\\d{9}$");

    private final SecretKey key;
    private final long ttlMillis;
    private final long impersonationTtlMillis;
    private final IdentityProvider identityProvider;
    private final PasswordEncoder passwordEncoder;
    private final TenantDirectory tenantDirectory;

    public LocalSsoProvider(
            @Value("${mido.jwt.secret:mido-pm-default-secret-key-please-change-in-prod}") String secret,
            @Value("${mido.jwt.ttl-millis:86400000}") long ttlMillis,
            @Value("${mido.jwt.impersonation-ttl-millis:1800000}") long impersonationTtlMillis,
            IdentityProvider identityProvider,
            PasswordEncoder passwordEncoder,
            TenantDirectory tenantDirectory) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        // HS256 要求密钥至少 256bit(32 字节)。提前显式校验，给出清晰提示。
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "mido.jwt.secret 长度不足：HS256 需至少 32 字节(256bit)，当前 "
                            + secretBytes.length + " 字节。请将 MIDO_JWT_SECRET 设为不少于 32 个字符。");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.ttlMillis = ttlMillis;
        this.impersonationTtlMillis = impersonationTtlMillis;
        this.identityProvider = identityProvider;
        this.passwordEncoder = passwordEncoder;
        this.tenantDirectory = tenantDirectory;
    }

    @Override
    public String login(String account, String rawPassword, String tenantCode) {
        // 1) 解析并校验租户（编码缺省回落自用租户），设置上下文使身份查询限定在该租户内
        Long tenantId = resolveTenant(tenantCode);
        if (!tenantDirectory.isLoginable(tenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "租户不可登录（不存在或已停用/到期）");
        }
        TenantContext.set(tenantId);

        // 2) 租户内按手机号/用户名加载身份（双登录）
        UserPrincipal principal = (account != null && PHONE.matcher(account).matches()
                ? identityProvider.loadByPhone(account)
                : identityProvider.loadByUsername(account))
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED, "账号或密码错误"));
        if (!passwordEncoder.matches(rawPassword, principal.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        if (STATUS_DISABLED.equalsIgnoreCase(principal.getStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已停用");
        }
        return issueToken(principal.getUserId(), tenantId, null, ttlMillis);
    }

    @Override
    public TokenPayload verifyToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            // jjwt 对小整数可能以 Integer 反序列化，统一按 Number 取值转 Long，避免类型转换异常
            Long tenantId = asLong(claims.get(CLAIM_TENANT));
            Long impersonatedBy = asLong(claims.get(CLAIM_IMPERSONATOR));
            // 兜底：历史/缺省令牌无租户声明时回落自用租户，避免空指针
            if (tenantId == null) {
                tenantId = tenantDirectory.defaultTenantId();
            }
            return new TokenPayload(userId, tenantId, impersonatedBy);
        } catch (JwtException | NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String refreshToken(String token) {
        TokenPayload payload = verifyToken(token);
        return payload == null ? null
                : issueToken(payload.userId(), payload.tenantId(), payload.impersonatedBy(), ttlMillis);
    }

    @Override
    public String issueImpersonationToken(Long userId, Long tenantId, Long impersonatedByAdminId) {
        return issueToken(userId, tenantId, impersonatedByAdminId, impersonationTtlMillis);
    }

    private Long asLong(Object claim) {
        return claim instanceof Number n ? n.longValue() : null;
    }

    private Long resolveTenant(String tenantCode) {
        if (!StringUtils.hasText(tenantCode)) {
            return tenantDirectory.defaultTenantId();
        }
        Long id = tenantDirectory.resolveIdByCode(tenantCode.trim());
        if (id == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "租户不存在");
        }
        return id;
    }

    private String issueToken(Long userId, Long tenantId, Long impersonatedBy, long ttl) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TENANT, tenantId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl))
                .signWith(key);
        if (impersonatedBy != null) {
            builder.claim(CLAIM_IMPERSONATOR, impersonatedBy);
        }
        return builder.compact();
    }
}
