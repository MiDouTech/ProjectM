# MCP 连接器（智能体平台对接）

> 让 Claude cowork / WorkBuddy 等智能体平台以「连接器」形式调用米多 PM 的能力。
> 连接器本质 = 远程 **MCP（Model Context Protocol）Server**：PM 把现有领域 Service 包装为 MCP 工具对外暴露。
> 实现位于接入层模块 `server/gateway-mcp`（定位等同 OpenAPI 网关，**不是 Provider**）。

## 1. 端点

| 用途 | 方法 | 路径 |
|---|---|---|
| 建立 SSE 连接 | GET | `/mcp/sse` |
| JSON-RPC 调用 | POST | `/mcp/message` |

传输：MCP 官方 Java SDK（`io.modelcontextprotocol.sdk` 0.11.2）+ Spring WebMvc SSE。
两端点落入既有「租户应用」安全链（`anyRequest().authenticated()`），未携带有效凭证返回 401。
生产部署要求 **HTTPS + 外网可达**（Claude 远程连接器强制 HTTPS）。

## 2. 认证（PAT 先行）

- 携带请求头 `X-API-Key: <PAT>`，复用既有开放平台 API Key 机制（`sys_api_key` / `ApiKeyAuthenticationFilter`）。
- PAT 绑定某用户：调用**等同该用户身份**，自动继承其租户隔离、数据范围与可见性。返回数据已按调用者范围过滤。
- 在「组织管理 → API Key」创建 PAT（需 `org:apikey:manage` 权限），明文仅创建时返回一次。
- **调用范围（scope）**：创建 PAT 时可指定 `scopes`（`mcp:read` 只读 / `mcp:write` 读写，逗号分隔）；
  缺省授予读写两档。**仅授 `mcp:read` 即可签发"只读连接器"凭证**，写工具将被拒绝。
  注意：scope 仅约束 MCP 工具调用，不约束 REST；如需彻底限制智能体，建议为其单建低权限专用账号再签 PAT。
- OAuth 2.1 标准授权流为后续阶段规划，届时与 PAT 并存。

## 3. 工具清单

### 只读（P0）
| 工具 | 说明 |
|---|---|
| `list_projects` | 分页查可见项目（可按类型/状态/关键字筛选） |
| `get_project` | 项目详情 |
| `list_my_projects` | 我参与的项目 |
| `query_tasks` | 分页查任务（按项目/负责人/状态/逾期筛选） |
| `get_task` | 任务详情 |
| `list_npss_reviews` | 某项目 NPSS 验收轮次列表 |
| `get_npss_review` | NPSS 轮次详情（含干系人评分） |

### 写（P1，直接执行 + 审计）
| 工具 | 说明 | 领域事件 |
|---|---|---|
| `create_task` | 在项目下创建任务，返回创建后的任务详情 | `task.created`（含指派则附 `task.assigned`） |
| `update_task_status` | 流转任务状态（受工作流校验约束） | `task.status.changed` |
| `assign_task` | 指派/改派任务负责人 | `task.assigned` |
| `add_comment` | 对任务/项目/目标加评论（支持 @ 提醒） | `comment.created` |

**写操作策略**：写工具直调对应领域 Service 落库生效，与用户走 REST 完全一致——同事务发 Outbox 领域事件、写审计日志、受租户与数据范围约束（业务域 CRUD 不设细粒度权限码，故 MCP 与 REST 授权等价）。

## 4. 在智能体平台添加连接器

1. 部署后获得 HTTPS 基址，连接器 SSE 地址为 `https://<host>/mcp/sse`。
2. 在平台「连接器 / MCP Server」配置中填入该地址。
3. 鉴权填自定义请求头 `X-API-Key: <PAT>`。
4. 连接成功后平台可列出上述工具并调用。

## 5. 治理与边界

- **scope 校验**：每次工具调用经 `McpToolGuard` 统一校验——只读工具需 `mcp:read`、写工具需 `mcp:write`；
  缺范围返回错误结果。仅作用于携带 PAT 的调用（JWT 登录用户直连放行）。
- **调用审计**：每次经 PAT 的工具调用落一条审计（`entityType=mcp`、`entityId=API Key 主键`、
  `action=mcp_invoke`、detail 含工具名/范围/结果），区分「经 MCP 调用」与具体工具、Key；
  与领域 Service 自身审计（如 `task.created`）并存互补。
- **每 Key 限流**：基于 Redis 固定窗口按 API Key 限流，阈值 `mido.mcp.rate-limit.per-minute`（默认 120/分钟）；
  Redis 故障时放行（fail-open），不阻断正常调用。
- **租户隔离 / 数据范围**：MyBatis-Plus 拦截器自动注入，工具无需也不得手写 tenant 条件。
- **线程模型**：WebMvc SSE 在 servlet 请求线程同步处理消息，ThreadLocal 形态的租户/安全/API Key 上下文对工具天然可见。

## 6. 暂未实现（后续）

- **幂等**：本仓库无通用 Idempotency-Key 组件（REST 的任务创建亦无），故 MCP 写暂不单独引入；如需跨端统一幂等，应作为独立横切任务。
- **scope 细粒度（每工具白名单）/ 三档 read/write/admin**：当前为粗粒度读/写两档。
- **创建 PAT 的前端 scope 选择项**：后端 `ApiKeyCreateDTO.scopes` 已支持，前端表单字段待补。
- **OAuth 2.1 授权流、Resources（文档/报表只读资源）、真实平台端到端联调**：后续阶段。
