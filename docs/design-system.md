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

### 1.5 状态映射表（系统级硬约定，所有状态标签据此着色）

| 业务状态 | 色 token | Element Plus tag type |
|---|---|---|
| 未开始 / 已注册 | info | `info` |
| 进行中 / 价值验收中 | primary | `primary` |
| 有风险 / 临期 / 结果验收中 | warning | `warning` |
| 逾期 / 阻塞 / 失败 | danger | `danger` |
| 已完成 / 已结案 / 成功 | success | `success` |

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

---

## 4. 全局布局骨架（Worktile 式三段，全系统统一）

```
┌──────────────────────────────────────────────────────────┐
│  顶栏 TopBar：Logo · 全局搜索 · 全局新建 · 消息铃铛 · 头像   │ 48px
├───────┬──────────────────────────────────────────────────┤
│       │  二级导航/筛选区（按模块变化）                       │
│ 左导航 ├──────────────────────────────────────────────────┤
│ 64/   │                                                    │
│ 200px │            主内容区（视图容器）                      │
│ 深色  │                                                    │
│       │   ←─ 点击行/卡 → 右侧详情抽屉(480px) 不离开列表 ─→   │
└───────┴──────────────────────────────────────────────────┘
```

- **左侧主导航**（深色 `--mido-nav-bg`，可收起为 64px 图标态）：工作台、项目、目标、审批中心、报表、文档、管理后台。对齐 Worktile 左导航。审批中心含「待我审批 / 变更台账」两个 Tab，变更不单列一级。
- **二级导航**：进入模块后出现。如"项目"模块二级为：我的任务、全部项目、立项审批、统计分析、报表。
- **右侧详情抽屉**：任务/项目/审批/目标详情统一走 `el-drawer`（宽 480px），左信息 Tab + 右活动流（评论/活动/流转/状态审批），对齐 Worktile 详情页。**禁止整页跳转看详情。**

---

## 5. 组件规范（Element Plus 映射 + 自研件）

### 5.1 直接用 Element Plus（仅换肤，不重造）

| 场景 | 组件 | 约定 |
|---|---|---|
| 主按钮 | `el-button type=primary` | 一屏只允许一个主按钮 |
| 表格 | `el-table` | 支持列配置、排序、固定列；表头底 `--el-fill-color-light` |
| 表单 | `el-form` | label 右对齐、必填红星、`Hibernate Validator` 错误回显在字段下 |
| 弹窗 | `el-dialog` | 用于轻量创建/确认；复杂详情用 drawer |
| 抽屉 | `el-drawer` | 所有"详情"统一走右抽屉 |
| 下拉/选人 | `el-select` + 自研 UserPicker | 选人支持成员/部门/角色三类（对齐 Worktile） |
| 消息/通知 | `el-message` / `el-notification` | 轻反馈用 message，需操作用 notification |
| 标签 | `el-tag` | 状态色严格走第 1.5 节映射表 |
| 步骤 | `el-steps` | 用于立项审批进度 |

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

**D. 管理后台 Admin**：
左栏：成员管理、角色管理（功能权限 + **数据范围**：本人/本部门/本部门及下属/全部/自定义，对齐 Worktile）、应用管理、组织架构（预留企微同步）、企业设置、审批模板管理、项目模板管理。

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
