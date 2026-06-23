# CLAUDE.md — 米多通用项目管理系统（mido-pm）

> 本文件是 AI 在本仓库工作的**最高约束**。每次生成代码前，先读相关 `docs/` 事实源。架构决策由人定，AI 在既定架构内填充实现，**不得擅自改技术栈、改表结构、改分层**。

## 0. 工作纪律（每次任务必守，优先级最高）
1. **先想后做**：接到任务先深度思考、**用自己的话复述需求**与边界、**识别风险**（影响范围/兼容性/数据安全），再执行；不确定就停下问人，不要猜架构。
2. **出方案先评审**：让出方案时只输出方案，**人审核通过后才开发**；大任务按 P0/P1/P2 拆分，每个优先级一个 commit。
3. **改动前先评估影响**：动任何东西前先用 Grep/Glob 评估影响范围；大文件优先 Grep（命中多先 `count`）、Read 用 offset/limit 分段。
4. **敏感内容禁止上库**：**严禁把配置性/敏感内容**（密钥、密码、token、企微/数据库/对象存储等真实凭证、内网地址、`.env`/私有证书等）提交或推送到 GitHub。一旦发现此类内容将被纳入提交，**先拒绝并提醒用户**，改用环境变量/占位符注入（仓库内只放默认占位值）。
5. **如实汇报**：测试失败要贴输出说失败；跳过的步骤要讲明；完成并自检无误才说做完。

## 1. 项目定位
米多业务侧通用项目管理系统。对标 Worktile 全功能，差异化护城河 = 立项审批引擎 + 干系人管理 + NPSS 两段式价值验收。战略：预留扩展 / 先自用再商用 / 先夯实基础再接入智能。

## 2. 技术栈（锁定，禁止替换）
- 后端：Java 17 LTS + Spring Boot 3.x + Spring MVC + Spring Security + JWT
- ORM：MyBatis-Plus 3.x（用代码生成器；**禁裸 JDBC、禁 JPA/Hibernate ORM**）
- 数据库：MySQL 8.0 + HikariCP
- 缓存：Redis 7.x；消息：RabbitMQ 3.x（事件驱动主通道）
- 分片：ShardingSphere 5.x（多租户预埋，阶段一不启用分片但表结构就位）
- 存储：S3 兼容（MinIO/OSS）
- 工具：Jackson、Hutool、Hibernate Validator、Swagger/OpenAPI 3.0
- 前端：Vue 3 + Element Plus + Pinia + Vue Router 4 + Vite + Axios
- 可视化：AntV G2/G6 + vuedraggable；甘特图允许引入轻量级开源专用库（如 frappe-gantt，MIT），其余可视化仍以 AntV 为准
- 部署：Docker + Docker Compose（阶段一）

## 3. 事实源文档（生成前必读对应文件）
- `docs/design-system.md` — 前端视觉/交互契约（颜色/间距/组件/布局/视图范式）。**写任何前端先读它**。
- `docs/data-model.md` — 完整 DDL。**写实体/Mapper 先读它**，以 DDL 为准。
- `docs/npss-rule.md` — NPSS 算分、权重模板、奖金硬校验、职级门槛。**写验收/奖金/审批 guard 先读它**。
- `docs/api-conventions.md` — REST/CRUD/分页/幂等/状态码/错误结构/多端契约。**写 Controller 先读它**。
- `docs/domain-events.md` — 领域事件清单（业务发布 ↔ AI/消息订阅）。**写任何写操作先读它**。

## 4. 架构分层（模块化单体 DDD）
```
server/
  common/        通用：响应包装/异常/分页/多租户拦截器/事件Outbox/Provider接口
  module/
    project/   task/   goal/   stakeholder/   verify(npss)/
    approval/  change/ cost/   collab/ doc/  report/   org(rbac)/
    ai/        (智能层,独立,只订阅事件,默认不启用)
    platform/  (平台域=SaaS 运营总后台：租户注册/套餐配额/订阅/平台账号RBAC/运营审计)
    # change=通用变更中心：受控变更单+审批编排，被改域经 ChangeApplier 端口回写，change 不反向依赖业务域
  provider/      identity/ sso/ approval/ message —— 四 Provider，local 实现先行，wecom 实现预留
```
每个 module 内分层：`controller / service / domain / mapper / entity / dto / event`。**跨域只能通过 Service 接口或领域事件，禁止跨域直接查表。**

> **平台域（platform）是跨租户的全局域**，与租户内业务域不同维度：管理「谁是租户/买了什么套餐/配额/到期/谁在运营」。其表**不带 `tenant_id`**、登记在 `MidoTenantLineHandler` 忽略名单、不参与多租户隔离（继承 `PlatformBaseEntity` 而非 `BaseEntity`）；平台账号体系独立于任何租户，走 `/api/v1/platform/**` + 独立 JWT 密钥（`PlatformTokenService`），与租户 SSO 物理隔离。**这是对下方规则 1/2 的唯一正式例外，业务域一律不得照搬。**

## 5. 硬性规则（违反即返工）
1. 所有业务表必带 `tenant_id`，查询经 MyBatis-Plus 多租户拦截器统一注入，**业务代码禁手写 tenant 条件**。（唯一例外：平台域 `platform` 全局表，见上方说明。）
2. 公共字段统一：`id`(雪花) / `tenant_id` / `create_by` / `create_time` / `update_by` / `update_time` / `is_deleted`(逻辑删除)。（平台域表无 `tenant_id`，其余公共字段一致。）
3. 任何**写操作**（增删改、状态流转）必须在同一事务内写 `sys_domain_event`（Outbox），事件名取自 `docs/domain-events.md`，不得自造。
4. 异步统一走 RabbitMQ；禁在业务线程内直接调企微/LLM 等外部副作用。
5. 外部能力（身份/组织、SSO、审批、消息推送）一律走 `provider/` 接口，**禁止业务层直接 import 企微 SDK**。
6. 校验用 Hibernate Validator（DTO 注解）；工具优先 Hutool；时间用 `LocalDate/LocalDateTime`。
7. 新接口必有 Service 单测；NPSS 算分、权重归零(<60)、受益方权重≥其他2倍、职级 guard 必须有测试。
8. 前端颜色/间距/状态着色只能用 `docs/design-system.md` 的 token 与 `StatusTag`，**禁裸 hex、禁裸 px、禁页面写死 tag type**。详情一律右抽屉，禁整页跳转。
9. 不改已发布的 migration（Flyway）；表变更追加新 migration。
10. REST 路径 `/api/v1/...`，响应包装、错误码、分页结构严格按 `docs/api-conventions.md`。

## 6. 命名与术语（统一，禁同义混用）
- 项目=Project(pm_project)；任务=Task；目标=Goal/KR；干系人=Stakeholder；验收=Verify/NPSS；立项审批=Approval；工时=WorkHour；费用=Cost。
- 日历=Calendar(pm_calendar)；日程=Schedule(pm_schedule，独立事件型，区别于任务日历视图)；参与人反馈=RSVP。简报=Briefing(待建，人工日/周/月报，区别于 PMO 度量 Report)。
- 项目类型：由租户在 `pm_project_type` 自配（取代原硬编码枚举 S/I/O）。内置种子 S=战略级 / I=创新级 / O=运营级（O 细分：常规运营/定向整改/专项督办）作为默认数据，可改名/停用/新增。立项职级门槛、是否走 NPSS、绑定审批流均为类型的可配置属性，业务代码禁再按 S/I/O 字符串硬编码分支（统一经 `ProjectTypeResolver` 解析类型后读属性）。
- 状态码、事件名、权限码集中登记，新增前先查重。

## 7. 工作方式
- 小步生成、逐层审查：DDL→Entity/Mapper→Service(+测试)→Controller→Swagger→Vue API→Vue 页面。
- 每完成一个模块跑一次构建与测试；每 2–3 模块 `/compact` 压上下文、`/review` 审变更。
- 不确定就停下问人，不要猜架构。

## Agent skills

> 供 mattpocock 系列工程 skill（`to-prd` / `triage` / `improve-codebase-architecture` 等）读取的本仓库配置。详见 `docs/agents/*`。

### Issue tracker
issue/PRD 记录在 **GitHub Issues**（仓库 `MiDouTech/ProjectM`）；外部 PR **不**作为请求面。本地用 `gh` CLI；Claude Code web 会话无 `gh`，改走 **GitHub MCP**（`mcp__github__*`，限 `midoutech/projectm`）。见 `docs/agents/issue-tracker.md`。

### Triage labels
默认五态：`needs-triage` / `needs-info` / `ready-for-agent` / `ready-for-human` / `wontfix`。见 `docs/agents/triage-labels.md`。

### Domain docs
单 context：根目录 `CONTEXT.md` + `docs/adr/`（暂未建，按需由 `domain-modeling` 懒创建）。现有领域术语事实源：本文件 §6 命名与术语、`docs/data-model.md`、`docs/npss-rule.md`。见 `docs/agents/domain.md`。
