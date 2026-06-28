# 租户端 Design System 优化方案（harvest from ops · 收敛为全局基座 + 品牌温度）

> 性质：**评审用方案，不含代码改动**。通过后再登记进 `docs/design-system.md`，并按 P0→P2 渐进落地。
> 结论先行：ops 层那句「将来若租户端收敛，可整体『晋级』为全局基座」——**现在就是收敛时机**。把 ops 里**非运营专属**的工程成果（密度档 / tabular 数字 / 批量条 / 骨架屏 / 危险隔离 / 抽屉分 Tab / 图表克制）**上移为全局基座**，租户与 ops 共同消费；ops 只保留「默认 compact + 深导航气质」这层皮肤差异。
> 取向（经评审确认）：**① 吸收 ops 工程基线**（抬一致性与精致度）**＋ ② 为面向客户的租户端补品牌温度**；**③ 密度档用户可手动切换并持久化**。
> 方法：盘点 `OpsLayout`/`tokens.css` ops 层 + `MainLayout` 与租户端 8 类页面现状；`/ui-ux-pro-max` 知识库（product/style/color/ux）。
> 治理铁律：**不另立色板**，新增 token 一律由现有 `--mido-*`/`--el-*` 派生（别名或 `color-mix`），零裸 hex、零裸 px。

---

## 0. 行业判断：吸收什么、克制什么、补什么

| 维度 | 租户端（customer-facing SaaS） | 运营后台（internal operator console） |
|---|---|---|
| 用户目标函数 | 客户：高效工作 + 品牌信任 + 情绪体验 | 内部：运营效率 + 可扫读 + 低误操作 |
| 信息密度 | **可调**：默认 comfortable（留白引导），数据密集视图可切 compact | 默认 compact（一屏信息最大化） |
| 视觉气质 | 专业克制为底 **＋ 适度品牌温度**（品牌色露出、插画空态、克制动效） | 克制、功能优先、几乎无装饰 |

**三条结论**：
1. **吸收（基线）**：密度档、tabular、批量条、骨架屏、危险隔离、抽屉分 Tab、图表克制——这些是「工程质量」，与目标函数无关，**应上移为全局基座**，两端共享。
2. **克制（守界）**：ops 的 *data-dense 默认*（13/14 正文、最小内边距、零装饰）**不作租户默认**；租户默认 comfortable，保留留白。深色侧导航已两端共用，不动。
3. **补（温度）**：租户面向客户，**在不破坏 token 唯一性的前提下**叠加一薄层 `--mido-brand-*` 温度（品牌渐变页头、插画空态、可点击卡抬升、欢迎态），全部派生自现有 token。

---

## 1. 从 ops「晋级为全局基座」的清单（核心治理动作）

把 `--mido-ops-*` 中**非运营专属**的部分，重命名/上移为全局，ops 改为引用全局：

| ops 现状（仅 ops 生效） | 晋级动作 | 晋级后（全局基座，两端共享） |
|---|---|---|
| `--mido-ops-line-height-compact: 1.4` | 上移 | `--mido-density-line-height-compact`（+ comfortable 档 1.5/1.6） |
| `--mido-ops-space-section/-card/-cell/-control`（compact 映射） | 上移为**双档** | `--mido-density-*`（comfortable / compact 两套，由 `data-density` 选择） |
| `data-density="compact"` 仅 OpsLayout 注入 | 上移 | 全局 `data-density` 机制，MainLayout 默认 `comfortable`，用户可切 |
| `el-config-provider :size` 仅 ops small | 上移 | 全局：密度档联动 Element size（comfortable=default / compact=small） |
| ops 的 tabular 数字约定 | 已是 §2 全局要求 | 补**落地清单**（见 §3），不是新规是补执行 |
| ops batchbar / 骨架屏 / 危险隔离 / 抽屉分 Tab | 抽象为共享组件/规范 | 见 §4 |

> `--mido-ops-*` 保留**仅剩气质项**：`--mido-ops-nav-bg/-canvas/-accent` 等皮肤别名继续存在并指向全局；密度/数字这类「工程基座」不再挂 ops 前缀。**ops 行为零回归**（值不变，仅换引用）。

---

## 2. 密度档（全局化 · 用户可切 · 持久化）

- **两档定义**：
  | 语义 | comfortable（租户默认） | compact（数据密集 / ops 默认） |
  |---|---|---|
  | 正文字号 | body 14 | secondary 13/14 |
  | 行高 | 1.5–1.6 | 1.4 |
  | Element size | default | small |
  | 区块/卡片/单元间距 | `--mido-space-5/5/3` | `--mido-space-4/3/2` |
- **机制**：根节点 `data-density`（CSS 钩子）+ `el-config-provider :size`（Element 组件联动）。组件按档取间距，**页面不写死**。
- **用户可切 + 持久化**：顶栏（或视图工具条）提供密度开关；偏好存 `localStorage`（P2 可升级为后端用户偏好）。默认 comfortable；任务表格/列表、费用、报表等数据密集视图可由视图配置默认 compact，用户仍可覆盖。
- **回归关注**：compact 档下控件点击热区不低于 32px；切换不引起布局抖动。

---

## 3. tabular 等宽数字落地清单（§2 已要求，补执行）

金额/预算/实际成本/工时/NPSS 分/编号/ID 一律 `.mido-mono`（`--mido-font-mono` + `tabular-nums`）+ **右对齐**（表格数字列）。落地点：

- 费用：`CostPanel`、费用列表金额列、预算 vs 实际。
- 工时：`WorkHourPanel`。
- NPSS：`NpssScoreCard`、报表 PMO 评价分。
- 项目/任务：编号、预算、完成率、逾期天数。
- 报表/仪表盘：KPI 卡数值、表格度量列。

---

## 4. 共享组件与交互规范（从 ops 抽象上移）

- **BatchBar（批量工具条）**：抽成 `web/src/components/BatchBar.vue`，浮于表上方，显示「选中 N + 批量动作 + 权限置灰」。复用于任务/项目/费用列表。
- **骨架屏优先于遮罩**：列表加载统一骨架屏（沿用 §8「列表用骨架屏，不用全屏遮罩」），补未落地页面。
- **危险操作视觉隔离**：删除/归档/奖金归零/恢复等 danger 色 + 图标 + 二次确认（讲清后果）+ 与常规操作留白隔离；热区 ≥32px；无权限置灰 + tooltip 不隐藏。
- **右抽屉分 Tab**：信息密集的详情抽屉（项目详情等）改抽屉内 Tab 分区 + 锚点，替代长纵向堆叠（任务详情已是 Tab 范式，对齐之）。
- **列设置 + 密度切换入口**：复用已存在的 `TableColumnSetting.vue`，统一接入列表型页面，并承载密度切换入口。
- **StatusTag / 中性 tag 边界**：延续硬约束——业务状态走 `StatusTag`，类型/级别/分类走 `el-tag info plain`，不借状态色表意。

---

## 5. 品牌温度层 `--mido-brand-*`（租户专属，全部派生）

> 仅租户布局作用域生效（ops 不引用），**零裸 hex**，均由 `--el-color-primary`/中性色派生；克制——温度是「点睛」不是「铺满」。

| 用途 | 建议 token | 派生来源 |
|---|---|---|
| 品牌渐变页头/欢迎态（工作台、登录、空项目引导） | `--mido-brand-gradient` | `linear-gradient` of `--el-color-primary` → `--el-color-primary-light-3` |
| 品牌浅底（选中/高亮区、引导卡） | 复用 `--el-color-primary-light-9` | 现有 |
| 可点击卡 hover 抬升 | 复用 `.mido-hoverable` + `--mido-shadow-hover` | 现有（尊重 reduced-motion） |
| 插画空态强调 | `EmptyState` 配品牌色插画 + 一句引导 + 主操作 | 现有组件，补品牌插画 |

温度的边界（避免变花哨，守 §0 原则 1「专业克制不炫技」）：
- 渐变/品牌色**只用于页头、欢迎态、空态引导、主 CTA**，不铺满工作区、不进数据密集区。
- 动效仅表达因果（抽屉/卡片抬升/加载），150–300ms，尊重 `prefers-reduced-motion`。
- 禁 emoji 当图标（延续 §10-8），插画用 SVG。

---

## 6. 数据可视化（克制用色，对齐 ops §7）

报表/仪表盘统一：进度 vs 阈值用 **Bullet/Progress**，趋势用 **Line/Area**，分布/对比用 **Bar**（>5 类禁饼），多序列靠线型(实/虚/点)区分（色盲友好），**色 + 图标/线型双编码**；图例可点切换、空数据态占位、grid 低对比、克制渐变阴影、reduced-motion 下关入场动画。租户端可在 KPI 卡保留**品牌色单序列**点缀，但数据对比区仍守语义色。

---

## 7. 可访问性（达标线，延续 §9）

正文/背景 ≥4.5:1（含品牌渐变上的文字需独立校验）；焦点环可见不移除；状态/图表色不单独表意；数字 tabular 防跳列；截断配 tooltip 全文；尊重 reduced-motion。

---

## 8. 与 `design-system.md` / ops 层的治理关系

| 类别 | 处置 |
|---|---|
| 色板 / 字体 / 圆角 / 阴影 / 动效 token | 100% 复用 `--mido-*`，唯一事实源 |
| 密度档（comfortable/compact） | **从 ops 上移为全局** `--mido-density-*` + `data-density` 机制 |
| tabular / batchbar / 骨架屏 / 危险隔离 / 抽屉分 Tab / 图表克制 | **上移为全局规范/共享组件**，两端消费 |
| `--mido-brand-*` 温度层 | **租户新增但派生**，仅租户作用域，ops 不引用 |
| `--mido-ops-*` | 收缩为**仅气质皮肤别名**（深导航/画布/accent），密度数字项退场 |

**登记方式**：通过后 `design-system.md` ① 新增「§X 密度档（全局）」；② §2 补 tabular 落地清单；③ 新增「§X 品牌温度层 `--mido-brand-*`」；④ §11 ops 层改为「引用全局密度档」表述。**保持 token 唯一性**：任何新 token 必可追溯到一个 `--mido-*`/`--el-*` 源。

---

## 9. 落地建议（分阶段，渐进无回归；每优先级一个 commit）

**P0 规范与底座（低风险，不改现有 token 值）**
- `design-system.md` 增「密度档（全局）」「品牌温度层」章节、§2 补 tabular 清单。
- `tokens.css`：新增 `--mido-density-*`（双档）与 `--mido-brand-*`（派生）；`--mido-ops-*` 密度项改为引用全局（值不变）。
- MainLayout 根节点注入 `data-density="comfortable"` + `el-config-provider`。
- 回归点：先在 1 个页面（如任务列表）验证密度切换无抖动、ops 零回归。

**P1 共享组件与高频页**
- BatchBar 共享组件；骨架屏补齐；危险操作隔离；tabular 落地（费用/工时/NPSS/任务/项目/报表）。
- 密度开关接入顶栏 + `localStorage` 持久化。
- 风险：批量条/危险隔离涉及多列表页 → 逐页灰度。

**P2 增强与温度**
- 品牌渐变页头/欢迎态（工作台、登录、空项目引导）+ 插画空态。
- 右抽屉分 Tab（项目详情）；列设置统一接入；图表克制用色落地；密度偏好升级为后端用户偏好。
- 风险：抽屉重构涉及大页 → 单独 PR + 联调。

**回归关注**：① 租户默认 comfortable、ops 默认 compact 各自正确；② 品牌渐变上文字对比度达标；③ StatusTag 与中性 tag 不混用；④ reduced-motion 与键盘路径；⑤ token 唯一性（无新色板、无裸 hex/px）。

---

*附：本方案由 ops 层成果盘点 + `/ui-ux-pro-max` 知识库（克制基线 + 品牌温度的平衡、密度档、图表 Bullet/Line/Bar、tabular 数字）综合。待评审通过后按 P0→P2 渐进实施，不一次性重构、全程不破坏 ops 现状。*
