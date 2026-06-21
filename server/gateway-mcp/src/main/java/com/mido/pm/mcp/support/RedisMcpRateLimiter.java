package com.mido.pm.mcp.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis 固定窗口（每分钟）的限流器实现。键形如 {@code mcp:rl:{keyId}:{epochMinute}}。
 * Redis 异常时<strong>放行（fail-open）</strong>，限流故障不阻断正常调用。
 */
@Component
public class RedisMcpRateLimiter implements McpRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisMcpRateLimiter.class);

    private final StringRedisTemplate redisTemplate;
    private final int perMinute;

    public RedisMcpRateLimiter(StringRedisTemplate redisTemplate,
                               @Value("${mido.mcp.rate-limit.per-minute:120}") int perMinute) {
        this.redisTemplate = redisTemplate;
        this.perMinute = perMinute;
    }

    @Override
    public boolean tryAcquire(Long keyId) {
        if (keyId == null) {
            return true;
        }
        String redisKey = "mcp:rl:" + keyId + ":" + (System.currentTimeMillis() / 60_000);
        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(70));
            }
            return count == null || count <= perMinute;
        } catch (RuntimeException e) {
            log.warn("MCP 限流计数失败，放行本次调用 keyId={}", keyId, e);
            return true;
        }
    }
}
