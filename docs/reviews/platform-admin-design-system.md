# 平台运营总后台 Design System 方案（operator console · 专属同源）

> 性质：**评审用方案，不含代码改动**。结论先行：认同"运营后台与租户端 design system 未必相同"，但应**分而不裂**——底层复用同一套 `--mido-*` token，叠加一层 `--mido-ops-*` 主题 + 紧凑密度档，形成专属气质。
> 方法：`/ui-ux-pro-max` 知识库检索（product/style/color/chart/ux）+ 结合当前 OpsLayout 与 8 个数据密集页面现状。
> 对标：Stripe 内部 Admin、Shopify 内部工具、Atlassian ADG admin 密度规范、Linear/Retool 等 operator console。
> 事实源治理：本方案落地后，`docs/design-system.md` 增设「运营后台（ops 层）」章节登记，不另立色板。

---

## 0. 行业判断：为何"分而不裂"

| 维度 | 租户端（customer-facing SaaS） | 运营后台（internal operator console） |
|---|---|---|
| 用户 | 客户，重品牌/转化/情绪 | 内部运营/客服/财务/超管，重效率/可扫读/低误操作 |
| 信息密度 | 中低，留白引导 | 高，data-dense，一屏尽量多有效信息 |
| 视觉气质 | 品牌色、插画、动效丰富 | 克制、功能优先、深色侧导航、动效极简 |
| 行业范式 | Polaris / 品牌站 | Stripe Admin / Retool / Linear / Atlassian admin |

**结论**：两者**目标函数不同**（转化 vs 运营效率），观感应区分；但完全割裂会破坏 token 唯一性、双倍维护、状态色不一致。正确姿势＝**单底座（`--mido-*` token）+ 双皮肤（ops 主题层）+ 密度档（compact）**。

---

## 1. 设计原则（data-dense internal admin）

1. **信息优先，密度可控**：默认 compact 密度，一屏信息量最大化；但用分组留白与层级避免"糊成一片"。
2. **可扫读 > 美观**：左对齐文本、右对齐数字、等宽数字（tabular）、固定关键列；视线沿列垂直扫描无跳动。
3. **状态即语言**：用状态语义色 + 图标/文案双编码（不靠颜色单独表意），运营一眼判断租户/订阅/任务健康度。
4. **低误操作**：危险操作（停用/注销/清除/重置密码/模拟登录）强区分、二次确认、与常规操作物理隔离；无权限按钮置灰不隐藏 + tooltip。
5. **批量与键盘效率**：多选批量、行内快捷、表头排序、筛选常驻、Enter 提交、Esc 关抽屉，减少鼠标往返。
6. **可追溯**：每个写操作有反馈（toast）+ 审计留痕；列表/详情可定位到"谁、何时、改了什么"。
7. **克制动效**：仅用动效表达因果（展开/抽屉/加载），时长 150–250ms，尊重 reduced-motion；杜绝装饰性动画。

---

## 2. 风格定位与气质

- **主风格：Data-Dense Dashboard（数据密集仪表盘）**。多表格/KPI 卡/指标、最小内边距、栅格化、空间高效——与 8 个运营页面（列表+指标+监控）天然契合，性能与可访问性（WCAG AA）双优。
- **辅风格：Minimalism / Swiss Style（瑞士国际主义）**。栅格驱动、功能至上、低装饰、强类型层级、几乎无阴影——为密集信息提供"秩序感"，避免数据密集沦为杂乱。
- **气质关键词**：克制、精确、专业、深色侧导航 + 浅色工作区（operator console 经典范式）。
- **明确规避**（知识库 anti-pattern）：花哨/拟物、渐变堆叠、装饰性插画、彩色滥用、无筛选的长列表、emoji 当图标。

---

## 3. 颜色系统（`--mido-ops-*` 叠加层，复用底座，零裸 hex）

原则：**不新增色板**，`--mido-ops-*` 一律由现有 `--mido-*` / Element 语义变量经 `color-mix()` 派生或语义别名，确保改一处全局生效。

| 用途 | ops token（建议命名） | 取值来源（语义引用，非裸 hex） |
|---|---|---|
| 侧导航底色 | `--mido-ops-nav-bg` | = `--mido-nav-bg`（深色，复用现有运营深导航） |
| 侧导航激活 | `--mido-ops-nav-active` | = `--mido-nav-text-active` |
| 工作区底色 | `--mido-ops-canvas` | = `--el-bg-color-page`（浅灰，降低长时间阅读疲劳） |
| 卡片/表面 | `--mido-ops-surface` | = `--el-bg-color` |
| 主强调（克制） | `--mido-ops-accent` | = `--el-color-primary`（仅主操作/激活态用，禁大面积铺） |
| 数据高亮/次强调 | `--mido-ops-data` | = `color-mix(in srgb, --el-color-primary 85%, --el-color-info)` |
| 状态-成功/正常 | 复用 `--el-color-success` | 经 StatusTag |
| 状态-警示/临期 | 复用 `--el-color-warning` | 经 StatusTag |
| 状态-危险/超限/逾期 | 复用 `--el-color-danger` | 经 StatusTag |
| 中性标注（类型/标签） | `--mido-ops-neutral` | = `--el-color-info`，`el-tag info plain` |
| 行 hover | `--mido-ops-row-hover` | = `--el-fill-color-light` |
| 分隔线 | `--mido-ops-border` | = `--el-border-color-lighter` |

要点：
- **强调色克制**：accent 只用于"一屏唯一主操作 + 导航激活 + 链接"，其余靠中性灰阶承载，避免页面"七彩斑斓"。
- **状态色不外露技术值**：一律经 `StatusTag`（状态）或 `el-tag info plain`（类型/中性），延续已修正的 P2-6 DS 边界。
- **深浅同测**：深色侧导航 + 浅色工作区两套对比度独立校验（见 §9）。

---

## 4. 排版与密度档

延续租户端字体家族（不换字体，保"同源"），仅在**密度与数字字形**上做 ops 特化。

- **字号阶梯**（复用现有 `--mido-font-size-*`）：标题 h2(页标题)/h3(区块) + 正文 secondary(14) + 辅助 caption(12)。运营后台**默认正文用 secondary(14)** 而非 16，提升密度；正文行宽不做 65–75 字限制（表格场景不适用）。
- **行高**：表格行用 `--mido-line-height-compact`（新增 ops 档，建议 1.4）；正文/抽屉用现有 1.5。
- **数字字形（关键）**：金额/配额/用量/计数/ID/IP 一律 `--mido-font-mono` + tabular-nums，**右对齐**，防止列内数字抖动跳列。
- **字重层级**：列标题/区块标题 600；正文 400；指标卡数值 700；状态/标签 500。
- **密度档定义**：
  - `comfortable`（抽屉/表单/详情）：行高 1.5，控件 default 尺寸。
  - `compact`（列表/监控/审计，默认）：行高 1.4，表格 `size="small"`，控件 small。
  - 档位由 OpsLayout 注入 `data-density`，组件按档取间距，不在页面写死。

---

## 5. 间距与布局

- **栅格**：工作区 12 栅格，左深导航固定宽（复用 `--mido-admin-nav-width`），内容区最大宽不锁死（运营要利用宽屏看更多列）。
- **紧凑档与 `--mido-space-*` 映射**（不新增数值，只做语义档）：
  | 语义 | comfortable | compact（ops 默认） |
  |---|---|---|
  | 区块间距 | `--mido-space-5` | `--mido-space-4` |
  | 卡片内边距 | `--mido-space-5` | `--mido-space-3` |
  | 表格单元 padding | `--mido-space-3` | `--mido-space-2` |
  | 控件间距 | `--mido-space-3` | `--mido-space-2` |
- **卡片**：指标卡用等高栅格、弱阴影（Swiss：尽量用边框而非重阴影）；圆角复用 `--mido-radius-md`。
- **表格**：斑马纹 + 行 hover 高亮 + 表头吸顶；操作列 `fixed="right"`；关键标识列（编码/名称）可 `fixed="left"`。
- **右抽屉**：复用 `--mido-drawer-width`，禁整页跳转（延续硬约束）；长抽屉分 Tab（见 §6）。

---

## 6. 组件取向

- **表格**：默认 compact + `size="small"`；表头排序（`sortable`）；筛选/搜索常驻顶部 bar；多选 + 批量工具条（已实现，统一为浮于表上方的 `batchbar`，显示选中数 + 批量动作 + 权限置灰）；列设置（显隐/密度切换）作为 P1 能力；空/加载/错误三态齐全（骨架屏优先于纯遮罩）。
- **StatusTag vs 中性 tag 边界**（延续 P2-6）：**状态**（trial/active/suspended/expired/closed、pending/done/failed、超限/正常）→ `StatusTag`；**类型/级别/分类**（收款/退款、公告 info/warning、套餐档）→ `el-tag info plain`，不借状态色表意。
- **右抽屉分 Tab**：租户详情抽屉信息多（基础/订阅配额/用量/导出注销/操作日志），改为抽屉内 Tab 分区 + 锚点，替代当前 7 区块纵向堆叠，降低找寻成本。
- **表单**：标签可见（非 placeholder 占位）、错误就近、必填星标、危险操作填原因；密码类带强度校验（已实现）。
- **三态**：列表加载用骨架屏；空态用统一 `EmptyState`（描述 + 主操作）；错误态用统一 `ErrorState`（失败 + 重试，已实现）。
- **危险操作视觉**：danger 色 + 图标 + 二次确认（讲清后果，已实现）；与常规操作间留白隔离；热区 ≥32px（修正当前 link 文字按钮热区偏小）。
- **按钮层级**：一屏一主 CTA（accent 实心）；次操作 default/plain；行内操作 link；**无权限置灰 + tooltip 不隐藏**（已实现）。

---

## 7. 数据可视化（AntV G2，克制用色）

| 场景 | 推荐图表 | 理由 / 用色 |
|---|---|---|
| 用量 vs 配额（用量监控/租户详情） | **Bullet（子弹图）/ Progress** | 空间高效、KPI vs 阈值一目了然；正常用中性/data 色，超限段用 danger，阈值线用 warning |
| 租户/订阅增长趋势（运营概览） | **Line / Area** | 时间轴趋势；单序列用 `--mido-ops-data`，多序列用线型(实/虚/点)区分而非纯靠色（色盲友好） |
| 租户状态分布 | **横向 Bar / 堆叠 Bar** | 类别比较优于饼图（>5 类禁饼）；用状态语义色 + 直接数值标注 |
| 收入收款/退款/净额 | **分组 Bar / KPI 卡** | 财务对比清晰；金额 tabular 等宽 |
| 多维对比（套餐能力对比，按需） | Radar（辅，必配分组 Bar 兜底） | 轴 ≤8；不熟悉时回退分组 Bar |

通用：图例常驻可点击切换、hover/点击 tooltip 给精确值、空数据态有占位、grid 低对比、克制渐变/阴影（Swiss 原则：数据 > 装饰）、`prefers-reduced-motion` 下入场动画关闭、提供数据表/CSV 兜底（已实现导出）。

---

## 8. 交互与动效

- **克制**：动效仅表达因果（抽屉滑入、行展开、加载、状态切换），150–250ms，ease-out 入 / ease-in 出；退场略快于入场；杜绝装饰动画。
- **键盘可达**：搜索 Enter 触发、表单 Enter 提交、抽屉/弹窗 Esc 关闭（有未保存改动时二次确认）、Tab 顺序合视觉顺序、表头排序键盘可触发。
- **批量与确认**：批量动作前显示影响条数 + 二次确认；破坏性批量提供"撤销"toast（P1）。
- **反馈**：写操作 100ms 内有视觉反馈；toast 3–5s 自动消失、`aria-live=polite` 不抢焦点。

---

## 9. 可访问性

- **对比度**：正文/背景 ≥4.5:1；大字/图标 ≥3:1；深色侧导航与浅色工作区**两套独立校验**；状态色文字达标（延续）。
- **焦点态**：所有可交互元素可见焦点环（2–4px），不移除 outline。
- **色不单独表意**：状态/图表一律"色 + 图标/文案/线型"双编码。
- **reduced-motion**：尊重系统设置，关闭非必要动画（登录页已示范）。
- **可读性**：数字 tabular 防跳列；长文本优先换行，截断必配 tooltip 全文。

---

## 10. 与租户端 design-system 的治理关系

| 类别 | 处置 | 说明 |
|---|---|---|
| 色板 / 间距 / 圆角 / 字体家族 / 阴影 / 动效 token | **100% 复用** `--mido-*` | 唯一事实源，ops 不另立 |
| `--mido-ops-*` 叠加层 | **新增但派生** | 均为现有 token 的语义别名 / `color-mix` 结果，零裸 hex |
| 密度档（compact/comfortable）| **ops 覆盖** | OpsLayout 注入 `data-density`，组件按档取间距 |
| StatusTag / EmptyState / ErrorState / 抽屉范式 | **复用同组件** | ops 只调密度与排布，不改组件语义 |
| 字号默认（正文 14 而非 16）、行高 compact、tabular 数字 | **ops 覆盖** | 仅在运营后台生效 |
| 深色侧导航 + L1/L2 分组 | **ops 专属** | 已落地，纳入规范 |

**登记方式**：`docs/design-system.md` 增设「§X 运营后台（ops 层）」，明确：① 列出全部 `--mido-ops-*` 及其派生来源；② 密度档定义与映射表；③ ops 专属规则（默认 compact、tabular 数字、深导航、StatusTag/中性 tag 边界）；④ 硬约束继承（禁裸 hex/px、状态走 StatusTag、详情右抽屉）。**保持 token 唯一性**：任何 ops 颜色必须能追溯到一个 `--mido-*` 源。

---

## 11. 落地建议（分阶段，渐进无回归）

> 全程不改租户端；仅在 OpsLayout 作用域内叠加 ops 层，按页灰度。

**P0（规范与底座，低风险）**
- 在 `docs/design-system.md` 增「运营后台 ops 层」章节（本方案固化为规范）。
- 定义 `--mido-ops-*` 叠加层与 `data-density` 档（仅新增 CSS 变量，不动现有 token）。
- OpsLayout 根节点注入 `data-density="compact"`，全局表格默认 `size="small"`。
- 风险点：密度变化影响所有 ops 表格观感 → 先在 1 个页面（审计日志）验证回归。

**P1（密度与可读性）**
- 金额/配额/用量/ID/IP 统一 tabular + mono + 右对齐。
- 租户详情右抽屉改 Tab 分区（基础/订阅配额/用量/导出注销/日志）。
- 列表骨架屏替换纯遮罩；危险操作热区与隔离优化。
- 用量监控/概览图表切 Bullet/Progress + Line，套用克制用色。
- 风险点：抽屉 Tab 重构涉及 TenantManage（最大页）→ 单独 PR + 联调。

**P2（效率增强）**
- 表格列设置（显隐/密度切换）、批量撤销 toast、列宽记忆。
- 概览指标卡可点击下钻到筛选后列表。
- 风险点：列设置需持久化（localStorage 或后端偏好），属新增能力。

**回归关注**：① 深色导航/浅工作区双对比度；② compact 档下控件点击热区不低于 32px；③ StatusTag 与中性 tag 不混用；④ reduced-motion 与键盘路径。

---

*附：本方案由 `/ui-ux-pro-max` 知识库（主风格 Data-Dense Dashboard、辅 Swiss Minimalism、图表 Bullet/Line/Bar、状态三色 + tabular 数字）结合当前 OpsLayout 与 8 页现状综合。待你评审通过后，再按 P0→P2 渐进实施，不一次性重构。*
