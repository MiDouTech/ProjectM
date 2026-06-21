-- V37 MCP 连接器治理：API Key 增加调用范围（scope）。粗粒度两档：mcp:read 只读 / mcp:write 读写。
--   存量 key 默认授予读写两档，保证既有调用不受影响；新 key 可仅授 mcp:read，用于签发"只读连接器"凭证。
--   scope 仅约束 MCP 工具调用；REST 仍以绑定用户权限为准（如需彻底限制智能体，另绑低权限专用账号）。
ALTER TABLE sys_api_key
  ADD COLUMN scopes VARCHAR(128) NOT NULL DEFAULT 'mcp:read,mcp:write'
  COMMENT 'MCP 工具调用范围(逗号分隔)：mcp:read 只读 / mcp:write 读写' AFTER status;
