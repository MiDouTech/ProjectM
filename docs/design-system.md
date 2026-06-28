# 米多项目管理系统 · 设计系统（Design System）v1

> 单一事实源（Single Source of Truth）：本文件是 AI 生成**所有前端页面**的视觉与交互契约。
> 任何页面、组件、配色、间距、状态都必须从这里取值，**禁止在业务页面里写裸色值、裸像素、裸字号**。
> 技术栈锁定：Vue 3 + Element Plus + Pinia + Vue Router 4 + Vite。可视化：AntV G2/G6 + vuedraggable。
> 设计基因：借鉴 Worktile 的信息架构与交互逻辑（详见《Worktile 调研》与本文件第 6、7 节）。

---

## 0. 设计原则（先立心法）

1. **专业克制，不炫技**：业务系统第一性是"高效完成工作"，信息密度 > 视觉惊艳。对齐 NNG 可用性、Apple HIG 清晰度。
2. **一致性高于一切**：同一种操作在任何模块长得一样、行为一样。这是本设计系统存在的唯一理由。
3. **借壳 Element Plus**：不重造组件。Element Plus 已有的（按钮、表格、表单、抽屉、对话框、下拉、消息）一律直接用，只通过 token 换肤；只有 Element Plus 没有的（看板、甘特、关系图、审批流设计器、权力利益矩阵）才自研。
4. **左导航 + 主内容区 + 右详情抽屉**：全局采用 Worktile 式三段布局，详情一律走右侧抽屉/侧栏，不做整页跳转，保留上下文。
5. **视图即配置**：同一份数据（任务）用看板/列表/表格/甘特/日历/仪表盘呈现，靠视图配置切换，不复制数据、不复制页面。
6. **多端预留**：所有 token、间距用相对单位与 CSS 变量；PC Web 为主战场，H5/小程序后续复用 token 体系。

---

## 1. 色彩 Tokens

以 Element Plus CSS 变量为基底覆盖，命名沿用 `--el-*`，业务自定义量用 `--mido-*` 前缀。

### 1.1 品牌主色（借鉴 Worktile 蓝，专业可信）

| Token | 值 | 用途 |
|---|---|---|
| `--el-color-primary` | `#3D6EFF` | 主操作、选中态、链接、进度 |
| `--el-color-primary-light-3` | `#6B8FFF` | hover |
| `--el-color-primary-light-5` | `#9DB6FF` | 次级 |
| `--el-color-primary-light-7` | `#CFDBFF` | 边框选中 |
| `--el-color-primary-light-9` | `#EDF2FF` | 选中底色、当前导航底色 |
| `--el-color-primary-dark-2` | `#3157D9` | active 按下 |

### 1.2 功能色（语义恒定，全局不得改语义）

| Token | 值 | 语义 |
|---|---|---|
| `--el-color-success` | `#2BA471` | 成功 / 已完成 / NPSS Success(9-10) |
| `--el-color-warning` | `#E37318` | 警告 / 有风险 / 临期 / NPSS Mixed(7-8) |
| `--el-color-danger` | `#D54941` | 危险 / 逾期 / 失败 / NPSS Failure(0-6) / 奖金归零 |
| `--el-color-info` | `#8A8F99` | 中性 / 未开始 / 已归档 |

### 1.3 中性色（文本与背景层级）

| Token | 值 | 用途 |
|---|---|---|
| `--el-text-color-primary` | `#1F2329` | 标题、正文主文本 |
| `--el-text-color-regular` | `#51565D` | 常规正文 |
| `--el-text-color-secondary` | `#646A73` | 辅助说明（白底 ≥4.5:1，过 WCAG AA） |
| `--el-text-color-placeholder` | `#8E949C` | 输入占位（白底 ≈3:1） |
| `--el-border-color` | `#DEE0E3` | 常规边框 |
| `--el-border-color-light` | `#EBEDF0` | 浅分隔线 |
| `--el-fill-color-light` | `#F5F6F7` | 区块底、表头底 |
| `--el-bg-color` | `#FFFFFF` | 卡片/内容区底 |
| `--el-bg-color-page` | `#F2F3F5` | 页面底 |
| `--mido-nav-bg` | `#1D2B45` | 左侧主导航深色底（Worktile 式） |
| `--mido-nav-text` | `#C9D1E0` | 深色导航项文字 |
| `--mido-nav-text-active` | `#FFFFFF` | 深色导航选中项文字 |
| `--mido-nav-active-bg` | `#2A3B5C` | 深色导航选中/hover 底色（active 另加 3px 主色左强调条）|

### 1.4 项目类型色（S/I/O 业务专用，强识别）

| Token | 值 | 含义 |
|---|---|---|
| `--mido-cat-s` | `#7C5CFF` | 战略级 S（紫，最高规格） |
| `--mido-cat-i` | `#11A2C7` | 创新级 I（青，探索） |
| `--mido-cat-o` | `#2BA471` | 运营级 O（绿，常规） |

### 1.4b 简报类型色（daily/weekly/monthly，类型标注专用）

| Token | 值 | 含义 |
|---|---|---|
| `--mido-brief-daily` | `#2BA471` | 日报（绿） |
| `--mido-brief-weekly` | `#11A2C7` | 周报（青） |
| `--mido-brief-monthly` | `#7C5CFF` | 月报（紫） |

> 简报类型是「类型标注」，不得挪用 §1.2 语义状态色（danger 红会被误读为告警），也不挪用 §1.4 项目类型色（避免 S/I/O 语义混淆），故设独立 token。

### 1.5 状态映射表（系统级硬约定，所有状态标签据此着色）

| 业务状态 | 色 token | Element Plus tag type |
|---|---|---|
| 未开始 / 已注册 | info | `info` |
| 进行中 / 价值验收中 | primary | `primary` |
| 有风险 / 临期 / 结果验收中 | warning | `warning` |
| 逾期 / 阻塞 / 失败 | danger | `danger` |
| 已完成 / 已结案 / 成功 | success | `success` |

### 1.6 品牌温度层（租户专属，全部派生，仅租户布局引用）

租户端面向客户，在「专业克制」基线上叠加一薄层品牌温度，**全部由主色派生、零裸 hex、ops 不引用**。克制点睛：**仅用于页头/欢迎态/空态引导/主 CTA，不铺满工作区、不进数据密集区**。

| Token | 取值来源（派生） | 用途 |
|---|---|---|
| `--mido-brand-gradient` | `linear-gradient(135°, --el-color-primary → --el-color-primary-light-3)` | 品牌渐变页头 / 欢迎态 / 空项目引导 |
| `--mido-brand-surface` | = `--el-color-primary-light-9` | 引导卡 / 高亮区浅底 |

> 温度边界：渐变上的文字一律白字并独立校验对比度（§9，正文级用 `--mido-nav-text-active` 取最高对比）；彩底页头的主操作用**白底按钮**（仍是一屏唯一 CTA，§5.1 规则的彩底例外）；动效仅表达因果、尊重 `prefers-reduced-motion`；禁 emoji 当图标（§10-8）。已落地：工作台欢迎页头（`Workbench` `.wb__hero`）。

---

## 2. 排版 Typography

| Token | 值 | 用途 |
|---|---|---|
| `--mido-font-family` | `-apple-system, "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif` | 全局 |
| `--mido-font-mono` | `"SFMono-Regular", Consolas, "Liberation Mono", monospace` | 编号/金额/代码 |
| 字号 H1 | `20px / 600 / 行高28` | 页面级标题 |
| 字号 H2 | `16px / 600 / 行高24` | 区块标题 |
| 字号 Body | `14px / 400 / 行高22` | 正文（系统默认） |
| 字号 Secondary | `13px / 400 / 行高20` | 辅助 |
| 字号 Caption | `12px / 400 / 行高18` | 标注、时间戳 |

> 数字（金额、预算、NPSS 分、编号）一律用 `.mido-mono`（`--mido-font-mono` + `font-variant-numeric: tabular-nums`），等宽数字防止数据列宽度跳动、便于对齐扫读。

---

## 3. 间距 / 圆角 / 阴影 / 层级

| 类别 | Token | 值 |
|---|---|---|
| 间距基数 | `--mido-space-1 … 6` | 4 / 8 / 12 / 16 / 24 / 32 px（4 的倍数） |
| 圆角 | `--mido-radius-sm / md / lg` | 4 / 6 / 8 px |
| 卡片阴影 | `--mido-shadow-card` | `0 1px 3px rgba(31,35,41,.08)` |
| 悬浮阴影 | `--mido-shadow-hover` | `0 4px 12px rgba(31,35,41,.10)`（可点击卡 hover 抬升）|
| 浮层阴影 | `--mido-shadow-pop` | `0 6px 24px rgba(31,35,41,.12)` |
| 动效时长 | `--mido-duration-fast / --mido-duration` | `120ms / 200ms`（micro-interaction 150–300ms）|
| 动效缓动 | `--mido-ease` | `cubic-bezier(.4,0,.2,1)`（入场 ease-out）|
| 聚焦环 | `--mido-focus-ring` | `0 0 0 2px var(--el-color-primary-light-7)`；全局 `:focus-visible` 用 2px 主色描边 |
| 布局尺寸 | `--mido-topbar-height / --mido-nav-width(-collapsed) / --mido-drawer-width / --mido-login-card-width / --mido-admin-nav-width` | 48 / 200(64) / 480 / 360 / 180 px |
| 层级 z-index | nav 1000 / 抽屉 2000 / 弹窗 2100 / 全局通知 3000 | 固定，禁随意取值 |

> 交互元素（按钮/卡片/导航/行）状态切换统一走 `--mido-duration` + `--mido-ease`；可点击卡片加 `.mido-hoverable`（hover 抬升，自动尊重 `prefers-reduced-motion`）。

### 3.1 密度档（全局基座，两端共享）

界面密度分两档，由根节点 `data-density` 选择，组件按 `--mido-density-*` 取间距/行高，**页面不写死**。

| 档位 | 适用 | Element 尺寸 | `--mido-density-line-height` | section / card / cell / control 间距 |
|---|---|---|---|---|
| `comfortable`（默认） | 租户端常规页（留白引导） | `default` | 1.6 | space-5 / 5 / 3 / 3 |
| `compact` | 数据密集视图、运营后台 ops | `small` | 1.4 | space-4 / 3 / 2 / 2 |

- **机制**：布局根节点注入 `data-density` + `<el-config-provider :size>`（联动 Element 组件尺寸）。租户端 `MainLayout` 默认 `comfortable`，顶栏提供切换并持久化（`localStorage: mido_density`）；ops `OpsLayout` 固定 `compact`。
- **治理**：`compact` 档数值 = 原 `--mido-ops-*` 密度值，ops 零回归；密度 token 已由 ops 专属上移为全局 `--mido-density-*`，不再设 `--mido-ops-*` 密度 token。
- **回归线**：`compact` 下控件点击热区 ≥32px；切换不引起布局抖动；金额/计数/ID 列 tabular 防跳列（§2）。

---

## 4. 全局布局骨架（Worktile 式三段，全系统统一）

```
┌──────────────────────────────────────────────────────────┐
│ Logo·项目管理 │ 模块二级导航(L2 横向) │ … 铃铛 · 头像        │ 48px
├───────┬──────────────────────────────────────────────────┤
│ 左导航 │  (L3 子导航 / 筛选区，按需出现，无则正文上贴)         │
│ 64/   ├──────────────────────────────────────────────────┤
│ 176px │            主内容区（视图容器）                      │
│ 深色  │                                                    │
│       │   ←─ 点击行/卡 → 右侧详情抽屉(480px) 不离开列表 ─→   │
└───────┴──────────────────────────────────────────────────┘
```

- **左侧主导航**（深色 `--mido-nav-bg`，可收起为 64px 图标态）：工作台、项目、目标、审批中心、报表、文档、日历、简报。对齐 Worktile 左导航。审批中心含「待我审批 / 变更台账」两个 Tab，变更不单列一级。**管理后台不在主导航列**：它是独立全屏布局（见 §7-D），由左导航底部固定入口新标签打开。
- **二级导航（L2）并入顶栏中段（全局统一）**：进入模块后，该模块的横向二级导航**渲染在顶栏中段空白处**（由 `WorkspaceShell` 经 `Teleport → #mido-topbar-nav` 注入），不再在内容区另起一行、不浪费顶部空白。如"项目"模块二级为：全部项目、项目集、统计分析等。L3 子导航/页面工具条留内容区顶部；无 L2/L3/工具条时正文直接上贴顶栏。
- **不重复导航名（全局统一）**：页面内容区**禁止再用 `mido-h1` 重复左导航/二级导航已呈现的模块名**（如「工作台」「全部项目」）。标题位仅保留**具体实体名**（如某个项目名、任务名）或直接省略，靠导航承载上下文。
- **右侧详情抽屉**：任务/项目/审批/目标详情统一走 `el-drawer`（宽 480px），左信息 Tab + 右活动流（评论/活动/流转/状态审批），对齐 Worktile 详情页。**禁止整页跳转看详情。**
- **工作区/详情整页的内部分区导航：统一顶部横向 Tab + 全宽主体**（如单项目工作区的概览/任务/甘特/目标/干系人/验收/费用/文件/活动）。用 `el-menu mode="horizontal"`（自带溢出「更多」）或 `el-tabs`，**禁止用左侧竖向导航占用主体宽度**；任务详情主体亦为顶部 `el-tabs`（信息/子任务/工时/附件/关联）+ 右活动栏。目的：把横向空间让给主体内容。
- **详情页信息架构（避免重复与喧宾夺主）**：
  - ①**顶部精简为 2 带**：标题/状态/迷你进度/主操作合一行 + 元信息一行；不堆「标题带+元信息带+大流程带+Tab带」4 带。生命周期改**迷你进度**（页头内「N/总 + 细条」，hover 弹完整阶段竖向 `el-steps`），状态仅由页头 `StatusTag` 表达一次，不占整带、不换行、不三重重复。次要操作（发起变更/归档/恢复）收进页头「更多」下拉，只留一个主操作（下一步）。
  - ②**Tab 分层 + 阶段智能显隐**：主导航只留浏览类（概览/任务/目标/干系人/甘特/费用/文件）；阶段动作类按阶段出现（立项仅草稿/审批中、验收仅结果验收及之后），平时不占位；设置(原信息)/活动/组件管理收进末尾**齿轮菜单**，不与导航抢位。
  - ③**「概览」是汇总/进展面，不复述页头已有字段**：顶部「项目健康」指标卡（完成率/逾期/干系人/实际vs预算/对齐目标），前端按既有端点聚合（任务看板含元类别 + 目标对齐计数 + 项目预算/实际成本），**不为此新增跨域后端依赖**（project 域不得反向依赖 task/cost，否则循环依赖）；右栏「当前阶段·下一步」卡收口立项/验收/流转入口。

---

## 5. 组件规范（Element Plus 映射 + 自研件）

### 5.1 直接用 Element Plus（仅换肤，不重造）

| 场景 | 组件 | 约定 |
|---|---|---|
| 主按钮 | `el-button type=primary` | 一屏只允许一个主按钮 |
| 表格 | `el-table` | 支持列配置、排序、固定列；表头底 `--el-fill-color-light`。**排序统一在表头切**（`sortable="custom"` + `@sort-change`），**筛选区/工具条禁止再放排序字段下拉与升降序按钮**；仅卡片等无表头视图保留一个精简排序入口作为例外 |
| 表单 | `el-form` | label 右对齐、必填红星、`Hibernate Validator` 错误回显在字段下 |
| 弹窗 | `el-dialog` | 用于轻量创建/确认；复杂详情用 drawer |
| 抽屉 | `el-drawer` | 所有"详情"统一走右抽屉 |
| 下拉/选人 | `el-select` + 自研 UserPicker | 选人支持成员/部门/角色三类（对齐 Worktile） |
| 消息/通知 | `el-message` / `el-notification` | 轻反馈用 message，需操作用 notification |
| 标签 | `el-tag` | 状态色严格走第 1.5 节映射表 |
| 步骤 | `el-steps` | 用于立项审批进度；统一 `simple` 紧凑态、只显节点名（不堆技术性 description），标题降至辅助字号，体量克制精致 |

### 5.2 自研组件（Element Plus 没有的，统一封装在 `web/src/components/`）

| 组件 | 说明 | 技术 |
|---|---|---|
| `KanbanBoard` | 看板，列=状态，卡可拖拽改状态 | vuedraggable |
| `GanttChart` | 甘特图，支持依赖连线、关键路径、里程碑菱形标记 | AntV G2/自研 |
| `GoalAlignTree` | 目标-KR-项目对齐关系图 | AntV G6 |
| `PowerInterestMatrix` | 权力利益矩阵四象限（干系人定位） | 自研 SVG |
| `ApprovalFlowDesigner` | 立项/审批流可视化设计器（节点+条件分支），对齐 Worktile 审批设计器 | 自研 |
| `NpssScoreCard` | NPSS 评分卡（0-10 打分 + 加权汇总 + 结果分级着色） | 自研 |
| `ViewSwitcher` | 看板/列表/表格/甘特/日历/仪表盘切换器 | 自研 |
| `UserPicker` | 成员/部门/角色三态选人（预留企微组织树） | 自研 |
| `StatusTag` | 业务状态标签，内置 1.5 映射表 | 封装 el-tag |
| `EmptyState` | 统一空状态（描述 + 可选主操作），落实 §8 空态规范 | 封装 el-empty |
| `BatchBar` | 列表多选批量工具条（已选 N + 动作插槽），浮于表上方，两端共享 | 自研 |

### 5.3 状态标签 StatusTag —— 全局唯一着色入口

**业务状态**（任务/项目/审批/NPSS 等生命周期状态，见 1.5 映射表）展示**必须**走 `<StatusTag :status="...">`，组件内部据第 1.5 节映射 type 与文案。**禁止业务页面用带语义色的 `el-tag type="danger/success/warning/primary"` 表达业务状态。**

**中性元标签**（非业务状态的标注，如「外部干系人」「会签/或签」「评分中」）允许直接用 `el-tag type="info" effect="plain"`（中性灰、plain 描边），不得使用语义色 type。即：语义色只能经 StatusTag；中性 info-plain 用于纯标注。

---

## 6. 多视图范式（同一份任务数据，六视图）

对齐 Worktile："看板 / 列表 / 表格 / 甘特 / 日历 / 仪表盘"，靠 `pm_view` 配置渲染，不建多套表。

| 视图 | 主用途 | 关键交互（借鉴 Worktile） |
|---|---|---|
| 看板 Kanban | 推进态流转 | 列=工作流状态，卡拖拽改状态；卡显示责任人头像、优先级、临期红点 |
| 列表 List | 快速浏览/批量 | 支持树状展开子任务、分组、行内编辑 |
| 表格 Table | 多字段对比/导出 | 列头可自定义（对齐 Worktile 表头设置）、多条件筛选（当…且…或）、导出 xlsx |
| 甘特 Gantt | 排期/依赖/关键路径 | 任务条拖拽改期、依赖连线、里程碑菱形、基线、关键路径高亮 |
| 日历 Calendar | 按截止日看 | 月/周切换，支持循环任务 |
| 仪表盘 Dashboard | 度量/下钻 | 燃尽、健康度、完成率、逾期率卡片 |

**视图设计器**（对齐 Worktile"视图设计器"四步）：选分组方式 → 选排序方式 → 设展示层级（展开到 N 级）→ 设查询条件（多条件且/或）。保存为个人视图。

---

## 7. 关键页面骨架范式（AI 据此生成各模块页面）

> 每个范式定义"页面结构 + 必备区块"，让所有模块页面长得一脉相承。

**A. 列表型模块页**（项目列表、任务列表、审批列表、费用列表通用）：
顶部操作条（新建 + 视图切换 + 排序 + 筛选 + 搜索）→ 主体视图区 → 行/卡点击 → 右抽屉详情。

**B. 详情抽屉**（项目/任务/目标/审批通用）：
头部（标题 + 状态标签 + 负责人 + 起止时间）→ 左 Tab 区（信息/子项/附件/依赖/工时）→ 右活动栏（评论 / 活动日志 / 流转 / 状态审批）。对齐 Worktile 任务详情。

**C. 工作台 Workbench**（登录首页）：
卡片化、可拖拽排序、可增删卡片。默认卡：我参与的项目、待我审批的立项申请、我负责的任务、待我审批的任务状态变更、我的目标、我的日程。对齐 Worktile 工作台（并改进其"卡片只能逐个添加、排版粗糙"的不足）。
> **边界原则**：工作台只放「个人行动」卡（我的待办/我负责/我发起/待我验收/被@），强调"我此刻要做什么"；**全局度量与趋势（完成率/逾期率/类型分布/燃尽/PMO NPSS 等）归"报表"模块**，不在工作台堆度量卡，避免两者职责重叠。护城河 NPSS 的露出：工作台「待我验收的项目」（个人）+ 报表「PMO 总体评价」（全局）。

**D. 管理后台 Admin（独立全屏布局 AdminShell）**：
- **独立全屏、新标签打开**：管理后台是独立布局 `AdminShell`（路由 `/admin`），**脱离主应用深色左导航**，由主导航底部「管理后台」入口新标签打开，正文全宽；**不得再嵌在 MainLayout 内形成双左导航**。
- **分组顶栏二级导航**：功能按 5 组归类（组织与权限 / 项目配置 / 流程与审批 / 数据与字段 / 系统），顶栏 L1 选组、L2 选组内项（横向，对齐 §4 顶栏导航标准），分组配置集中在 `router` 的 `adminNavGroups`。
- 角色管理含功能权限 + **数据范围**（本人/本部门/本部门及下属/全部/自定义）+ 字段权限。

**D-1. 配置页/管理后台 UX 约定（全局统一，忌技术语言）**：
1. **不外露技术原值**：`code`/权限码/枚举原值/组件 key 等**不作为主信息展示**，必要时降为副文本（`mido-mono mido-text-secondary` 小字）；标识类输入统一叫「标识」并附「系统内部使用，创建后不可更改」，编辑态锁定。
2. **权限/枚举用中文目录**：功能权限等以**中文分组可勾选清单**呈现（见 `RoleManage` 的 `PERM_CATALOG`），不让用户手输权限码；目录外的历史值保存时保留、不误删。
3. **内置项**：预置/种子项加「内置」徽章；**可改名/改色/调规则/停用，但不可删**（删除按钮 `disabled` + 说明），并以文案明确「内置仅作初始参考，同样可改」。
4. **颜色选择**用色块预览，不只显 token 名。
5. **列表三态 + 工具条**：空/加载/错误三态齐全；列表配搜索/筛选，排序走表头（§5.1）。
6. **受限操作**：无权限/不可用的操作**置灰 + tooltip 说明**，不隐藏。

**E. 立项审批**：
立项申请表单（按 S/I/O 类型动态字段）→ 审批流（固定/条件分支，可视化设计器）→ 审批进度 `el-steps` → 通过后项目进入"进行中"。**严肃场景：未通过审批不得进入执行态。**

---

## 8. 交互与反馈约定

- **空状态**：每个列表/视图必须有空状态插画 + 一句引导 + 主操作按钮（对齐 Worktile"当前暂无任务，点击新建"）。
- **加载**：列表用骨架屏，操作用按钮 loading，不用全屏遮罩。
- **危险操作**：删除/归档/奖金归零等二次确认（`el-messagebox`），文案讲清后果。
- **保存反馈**：行内编辑即时保存 + 轻 toast；表单保存后关闭抽屉并刷新列表。
- **权限缺失**：无权限的操作按钮置灰 + tooltip 说明，不隐藏（让用户知道有此能力）。

---

## 9. 无障碍与响应式（达标线）

- 文本对比度 ≥ WCAG AA（正文 4.5:1）。主色 `#3D6EFF` 配白达标。
- 可点击区域 ≥ 32×32px（表格行内图标按钮例外但需 hover 放大热区）。
- 键盘可达：抽屉 Esc 关闭、表单 Enter 提交、看板支持键盘移动（后续）。
- 断点：≥1280 主布局；<1280 左导航自动收起为图标态；移动端 H5 走独立栈（后续，复用 token）。

---

## 10. 给 AI 的硬性约束（写进 CLAUDE.md，每次生成前端都引用本文件）

1. 颜色只能用本文件 token，**禁止裸 hex**；新增色必须先在此登记。
2. 状态着色只能走 `StatusTag`，禁止页面内写死 tag type。
3. 详情一律右抽屉，禁止整页跳转。
4. 间距用 `--mido-space-*`，禁止裸 px（4 的倍数除外的临时值需注释理由）。
5. 能用 Element Plus 就不自研；自研件必须进 `components/` 且复用 token。
6. 列表页必须含空状态、加载态、错误态三态。
7. 任何金额/编号/分数用等宽字体。
8. **禁用 emoji 当图标**（导航/状态/按钮等结构性图标一律用 Element/SVG 图标，emoji 跨端不一致、不可 token 化）。
9. **图表配色克制且 token 化**：G2 等读不了 CSS 变量，一律经 `utils/chartTheme.js`（`chartColors`/`categoricalRange`）在运行时读 token，**禁裸 hex、禁用库默认色板**；单序列用品牌主色，分类优先「位置 + 直接数值标注」、色板用有限 token 派生色，状态用语义色，趋势多序列配线型区分（色盲友好），>5 类禁饼。

---

## 11. 运营后台 ops 层（operator console，专属同源）

> 平台运营总后台（`OpsLayout` + `/ops/**`）与租户端**分而不裂**：底层 100% 复用上方 `--mido-*` token，叠加一层 `--mido-ops-*` 与 compact 密度档，形成 data-dense 气质。完整方案见 `docs/reviews/platform-admin-design-system.md`。
> **治理铁律**：任何 `--mido-ops-*` 必须可追溯到一个既有 `--mido-*`/`--el-*` 源（别名或 `color-mix` 派生），**不另立色板**，以保 token 唯一性；将来若租户端收敛，可整体「晋级」为全局基座。

### 11.1 风格定位
- 主风格 **Data-Dense Dashboard**（数据密集、最小内边距、栅格化、信息优先）＋ 辅 **Swiss/Minimalism**（栅格秩序、低装饰、强类型层级）。
- 深色侧导航（`--mido-ops-nav-bg`）＋ 浅色工作区（`--mido-ops-canvas`），operator console 经典范式。

### 11.2 ops token 叠加层（定义见 `tokens.css`）
`--mido-ops-nav-bg/-nav-active/-canvas/-surface/-accent/-data/-neutral/-row-hover/-border`（颜色，均派生）。
密度档已上移为全局 `--mido-density-*`（见 §3.1），ops 仅经根节点 `data-density="compact"` 选择 compact 档，不再单设 `--mido-ops-*` 密度 token。

### 11.3 密度档（compact）
- 由 `OpsLayout` 注入：`<el-config-provider :size="small">`（全 Element 组件紧凑）＋ 根节点 `data-density="compact"`（CSS 钩子）；密度 token 取自全局 §3.1，ops 固定 compact 档。
- 默认正文 13/14、表格行高 1.4、表格 small；金额/配额/用量/ID/IP 用 `.mido-mono` + tabular 右对齐。

### 11.4 ops 专属约定（继承上方全部硬约束，叠加）
1. 运营列表默认 compact；筛选/搜索常驻；多选批量浮于表上方；表头排序。
2. 状态 → `StatusTag`；类型/级别/分类 → `el-tag info plain`（不借状态色表意）。
3. 强调色 `--mido-ops-accent` 仅用于一屏唯一主操作 + 导航激活 + 链接，禁大面积铺。
4. 危险操作（停用/注销/清除/重置密码/模拟登录）danger 色 + 图标 + 二次确认 + 与常规操作隔离；无权限置灰 + tooltip 不隐藏。
5. 导航按权限过滤 + L1/L2 分组（租户运营/商业化/平台治理）。
6. 图表克制：用量 vs 配额用 Bullet/Progress，趋势用 Line，分布用 Bar（>5 类禁饼），色 + 图标/线型双编码。
