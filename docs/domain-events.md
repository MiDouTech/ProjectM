# 领域事件清单（事实源 · 业务发布 ↔ AI/消息订阅契约）

> 任何写操作必须在同事务写 `sys_domain_event`（Outbox），由 RabbitMQ 投递。事件名取自本清单，**不得自造**。新增事件先在此登记。
> 命名规范：`<域>.<实体>.<动作>`，全小写点分。payload 为 JSON，含 `tenantId`、`entityId`、关键变更字段。

## 1. 项目域 project.*
| 事件 | 触发 | 主要订阅方 |
|---|---|---|
| `project.created` | 立项审批通过、项目创建 | 报表、AI(经验库) |
| `project.status.changed` | 状态机流转 | 消息(通知Leader/干系人)、AI(风险) |
| `project.registered` | PMO 注册完成 | 报表 |
| `project.closed` | 结果验收达标结案 | 定时(设NPSS到点)、报表 |
| `project.budget.exceeded` | 实际成本 > 预算 | 消息(预警)、AI(风险·R) |

## 2. 任务域 task.*
| 事件 | 触发 | 订阅方 |
|---|---|---|
| `task.created` | 建任务 | 消息(通知负责人) |
| `task.assigned` | 指派/改派 | 消息 |
| `task.status.changed` | 状态流转(含看板拖拽) | 报表、AI(风险) |
| `task.overdue` | 定时扫描逾期 | 消息(催办)、AI(风险) |
| `task.milestone.reached` | 里程碑任务完成 | 报表、消息 |

## 3. 立项/审批域 approval.*
| 事件 | 触发 | 订阅方 |
|---|---|---|
| `approval.submitted` | 提交立项申请 | 消息(通知审批人) |
| `approval.node.approved` | 单节点通过 | 消息(通知下一节点) |
| `approval.approved` | 全流程通过 | 项目(置已注册)、消息(通知申请人) |
| `approval.rejected` | 驳回 | 消息(通知申请人) |

## 4. 干系人/验收域 stakeholder.* / npss.*
| 事件 | 触发 | 订阅方 |
|---|---|---|
| `stakeholder.registered` | 干系人登记 | 报表 |
| `npss.review.started` | 定时唤醒价值验收 | 消息(通知干系人打分)、AI(价值重估·R) |
| `npss.scored` | 干系人提交评分 | 报表 |
| `npss.review.completed` | 评分汇总完成 | 报表(PMO汇总)、消息、AI |

## 5. 协作/费用/附件 collab.* / cost.* / attachment.*
| 事件 | 触发 | 订阅方 |
|---|---|---|
| `comment.created` | 评论(含@) | 消息(@提醒) |
| `cost.submitted` | 提报费用 | 审批、消息 |
| `cost.exceeded.budget` | 费用累计超预算 | 消息(预警)、AI |
| `attachment.uploaded` | 上传附件 | 报表、活动流 |
| `attachment.deleted` | 删除附件(逻辑删) | 活动流 |

## 6. 订阅方说明
- **消息(MessageProvider)**：阶段一站内信；激活后企微推送。按事件类型路由到 `pm_notification` 或企微应用消息。
- **AI 编排器**：阶段一不启用（事件照常入库，无消费者）；阶段二/三按 §5.2 能力顺序订阅。
- **报表**：聚合进 ES / 度量表，供仪表盘与 PMO 看板。

## 7. 事件 payload 示例
```json
// project.status.changed
{ "tenantId": 1, "projectId": 42, "from": "审批中", "to": "已注册",
  "operatorId": 7, "occurredAt": "2026-06-16T11:24:00" }
```
