package com.mido.pm.org.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * 创建 API Key 入参。绑定到当前登录用户；expireAt 可空表示长期有效。
 * scopes 为 MCP 调用范围(逗号分隔，mcp:read / mcp:write)，可空表示默认授予读写两档；
 * 仅授 mcp:read 可签发"只读连接器"凭证。
 */
public record ApiKeyCreateDTO(
        @NotBlank(message = "名称不能为空") String name,
        LocalDateTime expireAt,
        String scopes) {
}
