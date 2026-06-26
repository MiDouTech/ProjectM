# ADR-0002 task → 通用工作项迁移（阶段3，细化迁移与双写）

- 状态：**已接受（Accepted）**
- 日期：2026-06-26
- 上游：ADR-0001 §4 阶段3 / §5 迁移与兼容策略
- 影响范围：`pm_task` 数据模型；TaskService 写路径；WorkflowEngine
- 风险：🔴 高 / 含不可逆 schema 变更（仅追加列，不删存量）

> 本 ADR 细化阶段3 的迁移脚本、双写窗口与回滚，限定本阶段**只做"追加列 + 幂等回填 + 双写"**，
> **不翻转 source-of-truth**（字符串 `status`/`priority` 仍为主），不删旧列、不改前端。
> source-of-truth 切换与旧列废弃留后续阶段（兼容期验证无回归后另立 ADR）。

## 1. 决策

给 `pm_task` 追加 `type_id` / `status_id` / `priority_level_id` 三列（可空），
将存量任务关联到阶段1/2 的元数据（工作项类型 / 状态库 / 优先级模式），并在写路径**双写**新列；
读路径与展示**保持原字符串列**不变，故前端、报表、MCP 等下游零改动。

理由：一次性把 task 切到 id 体系会牵动 report/view/mcp/calendar/goal 全链路，回归面过大且不可逆。
分两步走——本阶段先"建列+回填+双写"建立并维持 id 与字符串的一致，下一阶段再在验证无回归后翻转读路径并清理旧列。

## 2. Schema 变更（V60，仅追加）

```
ALTER TABLE pm_task
  ADD COLUMN type_id           BIGINT NULL,  -- → pm_work_item_type
  ADD COLUMN status_id         BIGINT NULL,  -- → pm_status
  ADD COLUMN priority_level_id BIGINT NULL;  -- → pm_priority_level
```

## 3. 回填（幂等，可重跑）

按租户 JOIN 元数据回填；**仅对已种子元数据的租户命中**（如自用租户 tenant_id=1），
未种子租户三列保持 NULL（其行为继续走阶段2 的回落逻辑，不受影响）：

- `status_id`：`JOIN pm_status s ON s.tenant_id=t.tenant_id AND s.name=t.status`
- `type_id`：`JOIN pm_work_item_type wt ON wt.tenant_id=t.tenant_id AND wt.code='task'`（默认任务类型）
- `priority_level_id`：`JOIN pm_priority_mode pm(builtin=1)` + `pm_priority_level pl ON pl.mode_id=pm.id AND pl.level_value=t.priority`

回填为 `UPDATE ... JOIN ... SET`，重复执行得到相同结果（幂等）。

## 4. 双写（应用层）

- `create`：落库后解析并写 `type_id`（默认类型）、`status_id`（初始状态名→id）、`priority_level_id`（优先级值→level）。
- `changeStatus`：写字符串 `status` 的同时写 `status_id`（目标状态名→id）。
- `update`：优先级变化时写 `priority_level_id`。
- 解析全部 **best-effort**：解析不到（租户未种子）则该列写 NULL，不抛错、不阻断业务。
- 解析逻辑集中在 `WorkItemMetaResolver`，WorkflowEngine 复用之（消除重复）。

## 5. 兼容与回滚

- 字符串 `status`/`priority` 仍为权威来源，所有既有读路径与断言不变 → 本阶段对外行为零变化。
- 回滚：三列为追加且可空，停用双写即可；存量数据不动，无需删列。
- 一致性：双写保证新增/变更后 id 与字符串同步；存量由回填一次性对齐；
  阶段(后续)翻转读路径前可加一致性校验脚本（id 与字符串映射比对）。

## 6. 未决 / 后续

- 翻转 source-of-truth（读 status_id、`status` 降为派生/废弃）→ 后续阶段另立 ADR，需兼容期无回归证据。
- 阶段5 组件化视图、阶段4 关联关系将基于本阶段建立的 id 体系。
