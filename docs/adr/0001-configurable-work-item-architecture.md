# ADR-0001 可配置工作项 + 组件化视图架构（设计冻结）

- 状态：**已接受（Accepted）** — 架构决策已由产品负责人拍板
- 日期：2026-06-26
- 背景输入：Worktile「项目」模块产品调研报告
- 影响范围：task / view / field / report / mcp / calendar / goal 等几乎所有任务相关模块
- 关联文档：`docs/data-model.md`（DDL 事实源）、`docs/domain-events.md`（事件名）、`docs/api-conventions.md`、`docs/design-system.md`、`CLAUDE.md` §4/§5/§6

> 本 ADR 是本次重构的**设计冻结**：先固化目标架构、概念映射、迁移与兼容策略、阶段路线，
> 再按阶段填充实现。后续每阶段开工前回读本文件；表结构以 `docs/data-model.md` 追加为准。

---

## 1. 决策

将当前「单一 task 实体 + 固定状态机 + 硬编码优先级」升级为
**「一套可配置的工作项元数据模型 + 一套可拼装的组件化视图」**（Jira 式可配置工作项 + 工作流引擎，
结合国产协作工具的组件化视图），并保留我方差异化护城河（立项审批 / 干系人 / NPSS 两段验收）不变。

核心思路（借鉴 Worktile，标注为已采纳的设计原则）：
1. **元数据双层解耦**：数据定义（类型 / 字段 / 工作流 / 选项集）与展示（组件）彻底分离，同一份数据多视图复用。
2. **状态「元类别」收敛**：业务状态可无限扩展，但统一归约到 `未开始 / 进行中 / 已完成` 三元类别，保证跨项目统计口径一致。
3. **配置 → 应用闭环**：详情页表单、列表列、可选项、可流转状态，全部由配置驱动。
4. **选项集集中复用**：下拉字段引用共享「数据源」，集中维护。

**明确不照搬**：Worktile「按组件 × 按操作」超细权限矩阵默认全开放，配置负担过重；
我方在既有 RBAC + 字段级权限（view/edit）上**按需**补 operation 粒度，并提供默认权限模板。

---

## 2. 目标数据模型（新增元数据层）

> 表名遵循现有前缀约定（业务表 `pm_` / `sys_`），公共字段沿用 `BaseEntity`，
> 业务表必带 `tenant_id`（多租户拦截器注入）。最终 DDL 以 `docs/data-model.md` 追加为准。

| 实体 | 作用 | 取代 / 扩展 |
|---|---|---|
| `pm_data_source` + `pm_data_source_option` | 可复用选项集库 | 取代 `pm_field_def.options` 内联 |
| `pm_status` | 状态库：`name / color / meta_category(未开始/进行中/已完成) / group` | 取代 `TaskStatus` enum |
| `pm_priority_mode` + `pm_priority_level` | 优先级模式（4 象限 / 默认 / 缺陷…） | 取代前端 `TASK_PRIORITIES` |
| `pm_work_item_type` | **工作项类型** = 字段集 + 工作流 + 关联 + 模板 + 角色 | 全新核心轴 |
| `pm_work_item_type_field` | 类型 ↔ 字段绑定（顺序 / 必填 / 表单布局 / 是否系统） | 扩展 `pm_field_def` |
| `pm_workflow_transition` | 按类型的状态流转矩阵（from_status × to_status） | 取代 `TaskWorkflow` |
| `pm_relation_def` + `pm_relation` | 类型间「相关 / 派生」定义 + 实例追溯 | 全新（需求-任务-缺陷） |
| `pm_component` + `pm_project_component` | 组件库 + 项目安装实例（多实例 / 排序 / 配置） | 收编 `pm_view` |

`pm_task` 升级为通用工作项：增 `type_id`（→ `pm_work_item_type`）、`status_id`（→ `pm_status`）、
`priority_level_id`（→ `pm_priority_level`）；旧 `status` / `priority` 列在兼容期保留。

---

## 3. 概念映射（旧 → 新）

| 现状（硬编码 / 内联） | 目标（可配置元数据） | 迁移动作 |
|---|---|---|
| `TaskStatus` enum（未开始/进行中/已完成） | `pm_status` 行（meta_category 对应原 3 值） | 为每租户种子 3 条状态，旧 `status` 字符串映射到对应行 |
| `TaskWorkflow` 固定流转 | `pm_workflow_transition`（默认类型的矩阵） | 默认类型预置等价矩阵 |
| `TASK_PRIORITIES`（前端写死） | `pm_priority_mode` + `pm_priority_level` | 种子「默认优先级模式」，旧 `priority` 映射到 level |
| 单一 task | `pm_work_item_type` 内置「默认任务类型」 | 存量 task 全部归入默认类型 |
| `pm_field_def.options`（内联 JSON） | `pm_data_source`(+option) 引用 | 存量内联选项迁出为数据源（或保留内联兼容，二选一，见 §5） |
| `pm_view`（保存视图/筛选） | `pm_project_component`（看板/表格组件实例配置） | 收编为组件实例 |

> 术语登记（CLAUDE.md §6 要求集中登记，禁同义混用）：
> **工作项类型 = WorkItemType（`pm_work_item_type`）**；**状态库 = Status（`pm_status`）**，元类别 = MetaCategory；
> **优先级模式 = PriorityMode**；**数据源/选项集 = DataSource**；**组件 = Component（视图组件，区别于 provider）**；
> **关联关系 = Relation**（相关 related / 派生 derived）。

---

## 4. 阶段路线（每阶段独立评审 + 提交，阶段间设审批门）

| 阶段 | 内容 | 风险 | 是否动 task |
|---|---|---|---|
| **0** | 本 ADR 设计冻结 | 无 | 否 |
| **1** | 元数据底座：数据源 / 状态库 / 优先级模式（建表+CRUD+配置页，双轨并存） | 低·纯增量 | 否 |
| **2** | 工作项类型 + 工作流引擎（类型/字段绑定/流转矩阵，引擎替换 `TaskWorkflow`，内置默认类型） | 中 | 读路径接入 |
| **3** | task → 通用工作项迁移（type_id/status_id/priority），存量灰度双轨迁移 | 🔴 高·不可逆 | 是 |
| **4** | 关联关系（相关/派生）+ 需求-任务-缺陷追溯 | 中 | 扩展 |
| **5** | 组件化视图（组件库 + 项目安装实例，顶栏动态生成，收编 `pm_view`） | 中高 | 否 |
| **6** | 权限升级（按组件 + 按操作，含默认模板） | 中 | 否 |

---

## 5. 迁移与兼容策略（灰度双轨）

- **双轨期**：阶段 1–2 期间，新元数据表与旧 enum / 内联选项**并存**；task 写路径仍用旧 `status`/`priority`，
  新表只供配置与新功能读取，互不干扰。
- **阶段 3 切换**（不可逆步骤，单独评审）：
  1. 先以**新追加 migration** 给 `pm_task` 加 `type_id/status_id/priority_level_id`（可空），不删旧列。
  2. **数据回填**：存量 task 的 `status` 字符串 → 映射到种子 `pm_status` 行 id；`priority` → 默认模式 level id；
     type_id → 默认工作项类型。回填脚本幂等、可重跑。
  3. **兼容期**：读路径优先新列、回落旧列；写路径双写一段时间，确认无回归后再以后续 migration 废弃旧列。
  4. **回滚**：每步均为追加列 / 追加表，回滚 = 停用新读路径并切回旧列；不删存量数据。
- **选项集迁移**（数据源）：阶段 1 先支持「下拉字段引用数据源」**新增能力**；存量内联 options **保留可用**，
  不强制迁移（`pm_field_def` 同时支持 `data_source_id` 与内联 options，二者择一，data_source_id 优先）。
- **领域事件**（CLAUDE.md §3）：新写操作所需事件名先在 `docs/domain-events.md` 登记后再发布，**不自造**；
  纯配置 CRUD 若无对应事件，按既有 org/admin 配置类操作惯例处理（落审计日志），并在对应阶段评审是否补事件。

---

## 6. 权限模型

- 复用既有：RBAC 权限码 + 数据范围 + **字段级权限（view/edit，已落地）** + 项目角色（已落地）。
- 阶段 6 增量：
  - **按组件**：项目安装了某组件，对应角色才出现该组件的操作权限项。
  - **按操作**（按工作项类型）：在字段级之上补操作级（如「变更状态 / 分配负责人 / 登记工时」单独授权），
    提供「通用权限模式」默认模板，避免中小团队配置负担。

---

## 7. 影响面与风险

1. 核心模型重构，回归面覆盖 task/view/report/field/mcp/calendar/goal——每阶段须跑相关回归。
2. 阶段 3 迁移不可逆：靠"追加列/表 + 幂等回填 + 双轨兼容期"控风险，保证存量与历史活动零丢失。
3. 工期数月级，必须按阶段交付、阶段间验收，禁止一次性合并。
4. 战略权衡：本线工程量大，与"深化护城河（NPSS/立项/干系人）"争资源——已知悉并接受。

---

## 8. 未决 / 后续 ADR

- 阶段 3 启动前另立 ADR 细化迁移脚本与双写窗口。
- 组件化视图（阶段 5）与现有 `pm_view` 的收编细节，另立 ADR。
- 工作项类型的「角色设置」与阶段 6 权限矩阵的边界，阶段 6 评审时定稿。
