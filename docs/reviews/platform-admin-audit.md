# 平台运营总后台 产品诊断报告（SaaS 运营底座体检）

> 审计对象：`server/module-platform`（111 个 Java 文件）+ 前端 `web/src/views/ops/**`（8 页）+ 独立 `/ops` 路由与 `OpsLayout`。
> 审计视角：产品管理专家，聚焦**逻辑闭环、业界差距、信息架构、UI 体验**。
> 对标基准：Stripe Billing（计费/订阅）、钉钉 / 飞书管理后台（租户与账号治理）、Worktile / PingCode（同类 PM SaaS 运营台）。
> 方法：3 个并行审计代理深读后端 8 条核心链路、接口/数据模型、前端 IA/UI，关键结论已回源码二次核实。证据均标 `file:line`。
> 日期：2026-06-27。本报告**只诊断不改代码**，整改清单供评审后挑选实施。

---

## 一、总体结论与评分

**一句话结论**：平台域**功能广度已对标主流 SaaS 运营台（租户/套餐/配额/订阅/收入/公告/功能开关/API Key/导出/注销/审计一应俱全），但深度是"快速铺面"式交付——核心商用闭环存在多处断裂，安全与并发基线缺失，前端运营效率与三态规范欠账较多。当前形态适合自用 demo，距"经得起多租户商用"尚有一轮系统性打磨的距离。**

| 维度 | 评分 | 一句话判断 |
|---|---|---|
| 功能广度 | ★★★★☆ | 商用底座该有的模块基本齐全，路线图清晰 |
| 逻辑闭环 | ★★☆☆☆ | **到期失效、配额、注销合规、订阅不变量四条主链路均有断点** |
| 安全基线 | ★★☆☆☆ | **弱种子口令、无登录限流、自我提权、审计可篡改** |
| 架构合规 | ★★★★☆ | 平台域隔离、端口下沉、双登录态隔离做得规范，亮点明显 |
| 信息架构 | ★★★☆☆ | 隔离正确，但订阅/配额/用量无跨租户入口，全埋详情抽屉 |
| 前端 UI/效率 | ★★☆☆☆ | **全站无排序、无错误态、无按钮级权限；多页无分页/导出** |

**合规亮点（应保持）**：平台域无 `tenant_id`、继承 `PlatformBaseEntity`、独立 JWT（`typ=platform` 二次甄别）、端口下沉 `common` 保持模块无环、双登录态物理隔离、详情一律右抽屉无整页跳转、危险操作二次确认文案讲清后果、颜色全 token 化无裸 hex、自用租户 `tenant_id=1` 三层删除防护。

---

## 二、逻辑闭环诊断（核心链路逐条）

> 严重度：🔴 高 / 🟡 中 / ⚪ 低。下列均为代码事实，关键项已回源码核实。

### 2.1 租户生命周期与"到期失效"——**最严重的闭环断裂** 🔴

`trial → active → suspended / expired / closed` 这套状态机，**"到期"一环完全是空转**：

- 🔴 **无 expired 自动流转定时任务**。`PlatformMaintenanceScheduler` 只有 `processExports` 和 `purgeDue` 两个任务（`PlatformMaintenanceScheduler.java:28-56`），**没有任何按 `expire_at` 把到期租户置 `expired` 的调度**。`expire_at` 是写入即沉睡的死数据。
- 🔴 **到期租户仍能无限登录**。`isLoginable` 只看 `status` 字符串、不看 `expire_at`（`PlatformTenantDirectory.java:38-39`）。由于上一条没有自动流转，一个 `expire_at` 已过但 `status` 仍 `active` 的租户会永久通过登录校验——**"订阅到期"对客户零影响**。这是商用计费的致命缺口。
- 🟡 **状态机无流转 guard**。`changeStatus` 只校验"目标值在白名单 {active,suspended,closed}"，不校验 from→to 合法性（`TenantAdminService.java:155-162`）：可把 `closed`（已注销）直接拉回 `active`，可越级跳转。
- 🟡 **suspend 不即时生效**。`isLoginable` 只在登录入口判定一次（`LocalSsoProvider.java:69`），已签发 JWT（默认 24h）在过期前照常可用，无吊销/版本号机制。停用一个租户最长 24h 后才真正断访。

> **对标**：Stripe 用订阅状态机 + Webhook + 宽限期（`past_due → canceled`）驱动到期；钉钉/飞书到期即冻结写入并降级只读。本系统连"到期变更状态"这第一步都缺。

### 2.2 订阅与配额不变量 🔴🟡

- 🔴 **"每租户至多一条 active 订阅"无 DB 兜底、无并发锁**。仅靠应用层"先置旧 active 为 cancelled 再插新"（`PlatformSubscriptionService.java:56-69`），无 `SELECT...FOR UPDATE`，表上只有非唯一索引 `idx_sub_tenant`（`V27__platform_console.sql:78`）。两次并发 `bind` 可产生两条 active，不变量被打破。
- 🟡 **配额硬卡只覆盖 user/project**。全仓 `checkCanAdd` 仅 2 个调用点（`SysUserService.java:90`、`ProjectService.java:155`）。文档声称"task/storage 仅预警"，**实际代码里 task/storage 既不硬卡也无预警**——零校验。
- 🟡 **TOCTOU 竞态**：`checkCanAdd(resource, currentCount)` 由调用方先 count 再传入，count 与插入非原子，并发可双双越限。
- 🟡 **降级后存量超额无处理**。`bind` 降到更小配额只改订阅，不校验/不标记现有用量是否已超新上限，存量超额静默存在。
- 🟡 **无独立"退订/取消订阅"能力**，只能 `bind` 挤旧；`quota_override` 坏 JSON 静默吞掉退回默认（`PlatformQuotaService.java:52-54`），运营无感知。

### 2.3 收入台账与计费 🔴🟡

- 🔴 **退款不校验"不超过已收"**（`PlatformRevenueService.java:74-109`），可产生负净额。
- 🔴 **金额无 `@Positive`/`@DecimalMin`**（`RevenueRecordDTO.java:13` 仅 `@NotNull`），负数/零可入账。
- 🟡 **与订阅零关联**：`SysRevenueRecord` 无 `subscription_id`/`plan_id`（`V32__platform_p2_tables.sql:3-15`），只有自由文本 `contractNo`，无法按订阅对账。
- 🟡 **可自由改租户归属 + 无幂等 + 物理删**（`PlatformRevenueService.java:88-100`），重复提交重复入账，财务对账性不足。
- 🟡 **全表内存聚合**（`summary()` 全量 `selectList` 后 Java 循环，`:59-71`），数据量增长有性能/内存风险；**无币种字段**，多币种会被裸加和。

### 2.4 注销合规与数据清除 🔴🟡

- 🔴 **清除覆盖严重不全**：仅注册 4 个 purger（org/task/project/goal，约 15 表），而 `data-model.md` 有 70+ 张带 `tenant_id` 业务表。stakeholder/npss/cost/change/approval/doc/report/calendar 等域**无 purger，注销后数据残留**，违反合规目标。
- 🔴 **附件 OSS 对象不删除**，即便补齐 DB purger，对象存储文件仍残留。
- 🔴 **未强制"清除前先导出"**。`PlatformDeletionService` 全无 export 引用，可直接 request→到期物理删，数据永久丢失，合规链断裂。
- 🟡 `graceDays` 可传 0（`:50` 仅拦 <0），失去缓冲；调度**无分布式锁**，多实例重复清除；无双人复核/不可逆 confirm token。
- ✅ 正面：自用租户 `tenant_id=1` 三层防护（发起/查询/执行）严密。

### 2.5 模拟登录（Impersonate）🔴🟡

- 🔴 **refreshToken 把 30min 模拟令牌刷成 24h 且仍带 imp 声明**（`LocalSsoProvider.java:111-115`），短时约束被刷新链路绕过。
- 🔴 **只读拦截靠 HTTP 方法判定**：写操作若走 GET 即绕过，`/query` 后缀放行可被构造滥用，且**不覆盖异步/事件写**（`impersonatedBy` 不传播到 MQ 消费线程）（`ImpersonationReadOnlyInterceptor.java:38-44`）。
- 🔴 **令牌无 jti/无吊销/无绑定**，TTL 内可重放。
- 🟡 自用租户无模拟护栏（与注销的 `guardNotSelfUse` 基线不一致）；**仅审计"发起模拟"，模拟态内实际操作不审计**，事后追责粒度粗。

### 2.6 用量统计 🔴🟡

- 🔴 **`@Transactional` 自调用失效**：`snapshotTenant`(@Transactional) 被同类 `snapshotAll` 内部调用（`PlatformUsageService.java:45`），AOP 代理被绕过，单租户 4 个 upsert 非原子。
- 🔴 **无错误隔离**：`snapshotAll` for 循环无 per-tenant try/catch，单租户异常中断整批。
- 🔴 **N+1 性能**：每租户约 12 条 SQL 串行，千级租户必超时；upsert"先查后写"未用唯一键原子化，并发冲突。
- 🟡 **超限不持久化/不发事件/不告警/不阻断**，纯展示无闭环；`TenantContext` 缺失静默回落 `tenant_id=1` 会污染租户 1 快照。

### 2.7 运营审计覆盖 🔴🟡

- 🔴 **审计可被篡改**：Mapper 暴露 `BaseMapper` 全部 update/delete（`SysPlatformAuditLogMapper.java:9`），"防篡改"仅靠约定，无 DB 级 append-only/无 hash 链。
- 🔴 **平台管理员登录完全不审计**（无 `ADMIN_LOGIN` 动作），只覆盖式写 `last_login_at`，无登录历史。
- 🟡 **`TENANT_PURGED` 在调度线程记录，adminId/ip 为 null**——破坏性最强的动作丢失"谁执行"。
- 🟡 revenue/plan 物理删除审计 `detail=null`，被删内容无快照；审计普遍只记新值不记前后值（仅租户状态记了 from/to）；提权（角色变更）无 diff。

### 2.8 认证与账号 RBAC 🔴🟡

- 🔴 **无登录失败锁定/限流**（`PlatformAuthService.java:44-56`，实体无 fail_count/locked_until），可暴力破解。
- 🔴 **密码无任何策略**，create/reset 仅 `@NotBlank`，可设单字符弱密。
- 🔴 **超管种子弱口令 `superadmin123` 硬编码 + 明文写在 migration 注释**（`V28__platform_seed.sql:8-10`、`V36`），无 `must_change_password`、无首登强制改密。
- 🔴 **自我提权 / 越权**：任何持 `admin:manage` 者可给自己加 super_admin、重置任意（含超管）密码、停用最后一个超管——`update`/`replaceRoles`/`resetPassword` 对目标 id **零护栏**（`PlatformAdminService.java:80-111`），改自身角色下次请求即生效。
- 🟡 JWT 无吊销（改密/停用不使旧 token 失效）；无 2FA；权限码定义 15 个但 V28 种子只灌 9 个，缺 `revenue:*`/`announcement:manage`/`feature:manage`/`tenant:impersonate`。

---

## 三、对标业界最佳实践的差距

| 能力域 | 业界标杆做法 | 本系统现状 | 差距 |
|---|---|---|---|
| **订阅到期** | Stripe：状态机 + 宽限期 + Webhook 驱动降级/停服 | 到期无流转、不影响登录 | 🔴 链路缺失 |
| **配额计量** | 按资源全维度计量 + 软硬阈值 + 超限事件 | 仅 user/project 硬卡，task/storage 零校验 | 🔴 覆盖不全 |
| **计费对账** | 流水关联订阅/发票，退款不超已收，多币种 | 流水与订阅无关联、退款无校验、单币种 | 🟡 对账性弱 |
| **数据合规（注销）** | 全量导出 + 不可逆确认 + 全表/对象清除 + 留证 | 4/70 表覆盖、OSS 不清、不强制导出 | 🔴 合规链断 |
| **平台账号安全** | MFA + 登录限流 + 密码策略 + 最小权限 + 防自我提权 | 均缺失，存在自我提权 | 🔴 基线缺失 |
| **审计取证** | append-only + 防篡改 + 前后值 + 可导出 | 可被改删、缺登录审计、缺前值、无导出 | 🟡 取证不足 |
| **运营效率** | 列表排序/筛选/批量/导出齐全，跨实体看板 | 全站无排序、无批量、收入/审计无导出 | 🟡 效率欠账 |
| **可观测** | 写操作出领域事件驱动通知/计量 | revenue/usage 等未发 `sys_domain_event` | 🟡 违 §5.3 |

---

## 四、信息架构（IA）诊断

- 🔴 **订阅 / 配额 / 用量 / 导出 / 注销均无跨租户独立入口，全埋在租户详情抽屉**（`TenantManage.vue:112-227`）。运营高频场景——"看所有临期订阅""跨租户用量排行""所有导出任务进度"——必须逐个打开租户详情，无法横向看。**这是 IA 最大的结构性缺陷。**
- 🟡 **一级导航 7 项扁平平铺、无分组**（`router/index.js:66-73`），而 `design-system §7-D` 要求管理后台按组归类（L1 选组 / L2 选项）。建议分 3 组：**租户运营**（概览/租户/订阅/用量）｜**商业化**（套餐/收入/公告）｜**平台治理**（账号/角色/审计）。
- 🟡 **详情抽屉 7 区块纵向堆叠无 Tab/锚点**（`TenantManage.vue:88-230`），长抽屉信息找寻成本高，建议抽屉内分 Tab（基础/订阅配额/用量/导出注销/操作日志）。
- ⚪ 导航名与页内标题重复（"公告"vs"公告管理"）；✅ 详情右抽屉、双登录态隔离的 IA 隔离正确。

---

## 五、UI / 前端体验诊断

**三态与权限（硬约束级欠账）**
- 🔴 **全站 8 页无错误态占位**（接口失败只靠全局 toast，页面留白），违反 `design-system §8` 三态硬约束。
- 🔴 **全站无按钮级权限门控**：store 已拉取 `perms`（`store/opsUser.js:21`）却无一页使用，权限只靠后端，违反 §7「无权限置灰 + tooltip」。
- 🔴 **登录页明文写死默认超管账密 `superadmin/superadmin123`**（`OpsLoginView.vue:52,71`），生产泄露风险。
- 🔴 **运营角色/权限 CRUD 页缺失**（`AdminManage.vue` 只读角色供分配），运营侧 RBAC 前端不可治理。

**运营效率（普遍欠缺）**
- 🔴 **全站表格无排序**（无一处 `sortable`）；🟡 **收入/审计无导出**（最需对账取证的两页）；🟡 **全站无批量操作**；🟡 **Plan/Announcement/Admin 三页无分页**（全量 `list()`）。
- 🟡 状态切换（公告发布、账号启停、套餐启停）均需进抽屉改下拉，无行内快捷切换；筛选普遍缺日期范围。
- 🟡 **模拟登录直接覆盖本地 `TOKEN_KEY` 并 `window.open('/')`**（`TenantManage.vue:465-466`），会冲掉运营本人的租户登录态、跨标签串号。
- 🟡 401 不清登录态不跳登录（`request.js` 仅 toast），token 过期后停在空白页。

**设计系统一致性**
- 🟡 **StatusTag 被滥用为"类型/中性标注"着色**：收入 type(payment/refund)、公告 level 被渲染成状态语义色（`RevenueView.vue:42`、`AnnouncementManage.vue:11`），违反 §5.3（中性标注应用 `el-tag info plain`）。
- 🟡 配额上限 `-1=不限` 技术原值直接裸露给运营填（`PlanManage.vue:67`），应改 switch「不限/限额」+ 数字。
- ⚪ 金额未加 `.mido-mono` 等宽（§10.7）；登录卡 `border-radius:18px` 非 4 倍数裸 px；`--mido-admin-nav-width` 被挪用作输入框宽。
- ✅ 颜色全 token 化无裸 hex、危险操作二次确认文案到位、登录页可达性好。

---

## 六、分优先级整改清单

> 工作量为粗估（人日，1 人）。建议按 P0→P1→P2 推进，每个优先级一个 commit。

### P0｜商用与安全红线（必修，~8–11 人日）

| 项 | 问题 | 整改方向 | 工作量 |
|---|---|---|---|
| P0-1 | 到期完全空转（2.1） | 新增 expired 流转定时任务 + `isLoginable` 增加 `expire_at` 判定；suspend/expired 即时生效（token 版本号或短 TTL+续期校验状态） | 2 |
| P0-2 | 平台账号自我提权/越权（2.8） | 加护栏：禁止改自身角色、禁止停用/降级最后一个超管、reset 超管口令需更高权限 | 1.5 |
| P0-3 | 弱种子口令 + 无首登改密（2.8） | `must_change_password` 字段 + 首登强制改密；migration 注释去明文，口令走环境变量 | 1 |
| P0-4 | 登录无限流 + 无密码策略（2.8） | 失败锁定（fail_count/locked_until）+ 密码复杂度校验（DTO `@Pattern`） | 1.5 |
| P0-5 | 注销合规链断裂（2.4） | 强制"清除前已成功导出"前置；purger 覆盖全部带 tenant_id 域 + OSS 对象清除 | 2.5 |
| P0-6 | 登录页明文账密 + 全站无错误态（五） | 删登录页默认账密；列表统一接错误态占位 | 1 |

### P1｜闭环与运营效率（强烈建议，~7–9 人日）

| 项 | 问题 | 整改方向 | 工作量 |
|---|---|---|---|
| P1-1 | active 订阅无 DB 兜底（2.2） | 生成列 + 唯一索引（status=active 唯一）或 `bind` 加悲观锁 | 1 |
| P1-2 | 配额仅 user/project（2.2） | task/storage 接入 checkCanAdd 或软告警；降级存量超额标记 | 1.5 |
| P1-3 | 收入计费缺口（2.3） | 金额 `@Positive`、退款不超已收校验、关联 subscription_id、币种字段 | 2 |
| P1-4 | 用量定时任务三隐患（2.6） | 修自调用事务失效、加 per-tenant 错误隔离、upsert 走唯一键原子化 | 1.5 |
| P1-5 | IA：订阅/配额/用量无独立入口（四） | 新增"订阅管理""用量监控"跨租户列表页 + 临期/超限筛选 | 2 |
| P1-6 | 模拟登录安全（2.5） | refresh 不延长模拟令牌、自用租户禁模拟、模拟态操作审计 | 1 |

### P2｜规范与打磨（择期，~6–8 人日）

| 项 | 问题 | 整改方向 | 工作量 |
|---|---|---|---|
| P2-1 | 审计可篡改 + 缺登录/前值（2.7） | append-only 强制（授权/触发器）、补 `ADMIN_LOGIN`、关键动作记 from/to | 1.5 |
| P2-2 | 前端运营效率（五） | 表格排序、收入/审计导出、批量操作、Plan/Announcement/Admin 分页 | 2.5 |
| P2-3 | 按钮级权限门控（五） | 全站读 `perms` 做按钮置灰 + 导航按权限过滤 | 1.5 |
| P2-4 | 写操作未发领域事件（§5.3） | revenue/usage 等补 `sys_domain_event` Outbox | 1 |
| P2-5 | DS 一致性 + IA 分组 + Swagger | StatusTag 纠用、配额 -1 改 switch、导航分组、补 OpenAPI 注解 | 1.5 |

---

## 七、路线图建议

1. **先封红线（P0）**：到期失效、账号安全、注销合规是"对外商用"的准入门槛，应作为下一个迭代的唯一目标，一次性补齐。
2. **再补闭环（P1）**：订阅不变量、配额全维度、计费对账、用量稳定性，让"租户—订阅—配额—计费—用量"形成可信闭环。
3. **后做打磨（P2）**：审计取证、前端效率、权限门控、领域事件、设计系统一致性，提升运营体验与可观测。
4. **跨阶段原则**：所有写操作回归 `sys_domain_event`（§5.3）；所有"不变量"补 DB 级兜底而非只靠应用层；所有定时任务上分布式锁。
5. **不动既定架构**：平台域隔离、端口下沉、双登录态隔离、自用租户保护是亮点，整改全程保持。

---

*附：本报告基于 2026-06-27 代码快照；P0-1/P0-2/P1-2 等头部结论已回源码二次核实。具体实施前建议对每项再做一次影响范围 Grep。*
