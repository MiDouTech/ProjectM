package com.mido.pm.mcp.support;

/**
 * MCP 调用限流器：按 API Key 维度限制单位时间内的工具调用次数。
 */
public interface McpRateLimiter {

    /**
     * 尝试为某 Key 放行一次调用。
     *
     * @param keyId API Key 主键（null 表示非 Key 调用，放行）
     * @return true 允许；false 超出阈值需拒绝
     */
    boolean tryAcquire(Long keyId);
}
