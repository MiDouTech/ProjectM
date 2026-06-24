# 企业微信集成联调手册（wecom-integration）

> 适用：把米多 PM 的「身份/组织、SSO 登录、消息推送」从默认本地实现切到企业微信。
> 现状：**四 Provider 抽象已就位，企微实现默认关闭（Mock 仅打日志）**，本文档指导在你自己的环境配好凭证后逐项联调。
> 安全红线（CLAUDE.md §0.4）：**corp/secret/agent-id 等真实凭证一律走环境变量注入，严禁提交到仓库**；仓库内只保留空占位默认值。

## 1. 架构回顾

外部能力一律走 `provider/` 接口，业务层只依赖接口、不直接 import 企微 SDK（CLAUDE.md §5.5）。涉及三类：

| 能力 | Provider 接口 | 本地实现（默认） | 企微实现 | 客户端 |
|---|---|---|---|---|
| 消息推送 | `MessageProvider` | 站内信 `pm_notification` | `WecomMessageProvider` | `WecomMessageClient` |
| SSO 登录 | `SsoProvider` | `LocalSsoProvider`（JWT） | `WecomSsoProvider`（占位，P2 注册 Bean） | `WecomSsoClient` |
| 身份/组织 | `IdentityProvider` | 本地用户表 | `WecomIdentityProvider`（占位，P2） | `WecomContactClient` |

> 消息域 `WecomMessageProvider` 已 `@Component` 注册，由开关 `enabled` 决定真实外呼还是 Mock 日志；SSO/通讯录的企微 Provider 为占位类，P2 才注册为 Bean。

## 2. 配置键一览（application.yml → 环境变量）

```yaml
mido:
  wecom:
    corp-id: ${MIDO_WECOM_CORP_ID:}            # 企业 ID（CorpID），三类能力共用
    message:
      enabled: ${MIDO_WECOM_MSG_ENABLED:false} # 消息推送总开关
      secret:  ${MIDO_WECOM_MSG_SECRET:}       # 应用 Secret
      agent-id: ${MIDO_WECOM_MSG_AGENT_ID:}    # 应用 AgentId
    sso:
      enabled: ${MIDO_WECOM_SSO_ENABLED:false} # 扫码/OAuth 登录开关
      secret:  ${MIDO_WECOM_SSO_SECRET:}
      agent-id: ${MIDO_WECOM_SSO_AGENT_ID:}
    contacts:
      enabled: ${MIDO_WECOM_CONTACTS_ENABLED:false} # 通讯录同步开关
      secret:  ${MIDO_WECOM_CONTACTS_SECRET:}
```

**默认值全部为空/false** → 不配置即维持本地实现，系统照常运行（先夯实基础再接入智能）。

注入方式（示例 docker-compose / .env，**不入库**）：

```bash
export MIDO_WECOM_CORP_ID=ww************
export MIDO_WECOM_MSG_ENABLED=true
export MIDO_WECOM_MSG_SECRET=********
export MIDO_WECOM_MSG_AGENT_ID=1000002
```

## 3. 联调步骤

### 3.1 消息推送（最易先联调）

1. 企微后台建「自建应用」，记下 `AgentId` + `Secret`；可见范围含联调用户。
2. 配置可信域名/IP 白名单（出口 IP 加入企微「企业可信 IP」）。
3. 注入 `MIDO_WECOM_CORP_ID / MIDO_WECOM_MSG_*`，置 `MIDO_WECOM_MSG_ENABLED=true`，重启后端。
4. 触发任一会发消息的领域事件（如指派任务 `task.assigned`）→ 观察企微应用是否收到。
5. **Mock 对照**：`enabled=false` 时 `WecomMessageProvider.send` 仅打印 `[Mock企微] 预演推送 ...` 日志、不外呼——可据此先验证「谁在什么事件下该被通知」链路，再开真实开关。

> 注意：站内信与企微推送由 `MessageProvider` 路由统一；切企微不影响 `pm_notification` 落库逻辑。

### 3.2 SSO 登录（P2 激活）

1. 企微「自建应用 → 网页授权及 JS-SDK」配置可信回调域名，前端回调路由：`/wecom-callback`（`WecomCallbackView.vue` 已就位）。
2. 注入 `MIDO_WECOM_SSO_*`、置 `MIDO_WECOM_SSO_ENABLED=true`。
3. 把 `WecomSsoProvider` 注册为 Bean（当前占位，方法抛 `UnsupportedOperationException`）：建议加 `@ConditionalOnProperty(name="mido.wecom.sso.enabled", havingValue="true")`，与 `LocalSsoProvider` 用 `@ConditionalOnMissingBean`/`@Primary` 协调择一生效。
4. 流程：前端跳企微授权 → 回调带 `code` → `WecomSsoClient` 用 code 换 `userid` → 映射 `sys_identity_map` 到本地用户 → 签发本地 JWT（与 `LocalSsoProvider` 同一套 `mido.jwt.*`）。
5. 联调校验：未注册用户的 `userid` 应走「身份映射缺失」分支（拒绝或引导绑定），不得静默放行。

### 3.3 通讯录/组织同步（P2 激活）

1. 企微「通讯录同步」助手获取 Secret；注入 `MIDO_WECOM_CONTACTS_*`、置 `MIDO_WECOM_CONTACTS_ENABLED=true`。
2. 注册 `WecomIdentityProvider` 为 Bean（同上条件注册范式）。
3. `WecomContactClient` 拉部门/成员 → 映射到 `sys_dept`/`sys_user` + `sys_identity_map`（企微 userid ↔ 本地 userId）。
4. 联调校验：同步幂等（重复拉取不产生重复用户）、离职/调岗的增量处理、`tenant_id` 正确归属。

## 4. 验证清单

- [ ] `enabled=false` 时系统完全走本地实现，无任何企微外呼（看日志无真实 HTTP）。
- [ ] 消息：真实开关后，目标事件能在企微应用收到，标题/正文正确、@到正确的人。
- [ ] SSO：企微扫码 → 回调 → 换 userid → 身份映射命中 → 签发本地 JWT → 正常进入。
- [ ] 身份映射缺失/未授权用户被正确拦截，不绕过登录。
- [ ] 通讯录同步幂等、增量、租户归属正确。
- [ ] 凭证仅存在于环境变量/密钥管理，仓库与日志中无明文（日志对 agentId/secret 做掩码）。

## 5. 回退

任一能力联调异常，将对应 `MIDO_WECOM_*_ENABLED` 置回 `false` 重启即回退到本地实现，业务不中断。

## 6. 待办（代码侧，本手册外）

- `WecomSsoProvider` / `WecomIdentityProvider` 的真实实现与条件注册（当前占位）。
- 可选：管理后台「企微自检」入口（一键发一条 Mock/真实测试消息验证链路），便于运维联调。
- 身份映射绑定页（首次企微登录引导绑定本地账号）。
