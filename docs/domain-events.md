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
| `project.deleted` | 删除项目(逻辑删) | 目标(清理悬挂对齐)、报表、活动流 |
| `project.budget.exceeded` | 实际成本 > 预算 | 消息(预警)、AI(风险·R) |

## 1.1 项目类型域 project_type.*
| 事件 | 触发 | 主要订阅方 |
|---|---|---|
| `project_type.created` | 租户新建项目类型 | 报表、活动流 |
| `project_type.updated` | 项目类型配置变更（含重新启用） | 报表、活动流 |
| `project_type.disabled` | 项目类型停用 | 报表、活动流 |

## 1.2 目标域 goal.*
| 事件 | 触发 | 主要订阅方 |
|---|---|---|
| `goal.created` | 新建目标/KR | 报表、活动流 |
| `goal.updated` | 编辑目标/KR | 报表、活动流 |
| `goal.deleted` | 删除目标(连带删其对齐链) | 报表、活动流 |
| `goal.progress.changed` | KR 量化进度变化(手动改值/项目进度自动汇总反写) | 报表(OKR看板)、AI |
| `goal.aligned` | 目标对齐到 project/task | 报表(目标-项目贯通) |
| `goal.unaligned` | 解除对齐(含 project/task 删除清理) | 报表 |

## 2. 任务域 task.*
| 事件 | 触发 | 订阅方 |
|---|---|---|
| `task.created` | 建任务 | 消息(通知负责人) |
| `task.assigned` | 指派/改派 | 消息 |
| `task.status.changed` | 状态流转(含看板拖拽) | 报表、AI(风险) |
| `task.deleted` | 删除任务(逻辑删，含批量) | 报表、活动流 |
| `task.overdue` | 定时扫描逾期 | 消息(催办)、AI(风险) |
| `task.milestone.reached` | 里程碑任务完成 | 报表、消息 |
| `workhour.logged` | 登记/修改工时(预估/实际) | 报表(工时统计) |

## 3. 立项/审批域 approval.*
| 事件 | 触发 | 订阅方 |
|---|---|---|
| `approval.submitted` | 提交立项申请 | 消息(通知审批人) |
| `approval.node.approved` | 单节点通过 | 消息(通知下一节点) |
| `approval.approved` | 全流程通过 | 项目(置已注册)、变更(应用变更 bizType=change)、消息(通知申请人) |
| `approval.rejected` | 驳回 | 项目(回草稿)、变更(置驳回)、消息(通知申请人) |
| `approval.withdrawn` | 发起人撤回(审批中) | 项目(回草稿)、变更(置撤回)、消息(通知审批人) |
| `approval.transferred` | 审批人转交待办 | 消息(通知受让人) |
| `approval.node.skipped` | 节点审批人解析为空，自动跳过 | 消息(告警PMO/管理员) |

## 3.1 变更域 change.*（通用变更中心）
> 受控变更：改业务基线须走变更单，复用审批引擎(bizType=change)。被改实体域经 ChangeApplier 端口回写，变更域不反向依赖业务域。
| 事件 | 触发 | 订阅方 |
|---|---|---|
| `change.requested` | 提交变更单(必审/免审) | 消息(通知审批人)、活动流 |
| `change.applied` | 变更生效(回写被改实体) | 被改域(联动)、报表、活动流 |
| `change.rejected` | 变更驳回/撤回，未生效 | 消息(通知发起人)、活动流 |

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

## 5.1 文档域 doc.*（项目知识库：在线文档 + 版本）
| 事件 | 触发 | 订阅方 |
|---|---|---|
| `doc.created` | 新建文档/目录节点 | 活动流 |
| `doc.updated` | 文档标题/属性变更 | 活动流 |
| `doc.version.created` | 保存正文生成新版本 | 报表、活动流 |
| `doc.moved` | 节点移动/改父或排序 | 活动流 |
| `doc.deleted` | 删除文档/目录(逻辑删) | 活动流 |

## 5.2 日历/日程域 calendar.*（独立事件型日程）
| 事件 | 触发 | 订阅方 |
|---|---|---|
| `calendar.schedule.created` | 新建日程（payload 含 participantIds） | 消息(邀请通知·P1)、活动流 |
| `calendar.schedule.updated` | 编辑日程 | 消息(变更通知·P1)、活动流 |
| `calendar.schedule.deleted` | 删除日程(逻辑删) | 消息(取消通知·P1)、活动流 |
| `calendar.rsvp.responded` | 参与人 RSVP 反馈(参加/暂定/谢绝) | 消息(通知组织者·P1) |
| `calendar.reminder.due` | 定时扫描到点提醒(提前 N 分钟) | 消息(提醒参与人·P2) |

> 说明：阶段一事件照常入库，消息订阅（邀请/变更/RSVP/提醒 站内信）为后续接入；事件已发布、消费者待建。

## 5.3 简报域 briefing.*（人工日/周/月报，区别于 PMO 度量 report）
| 事件 | 触发 | 订阅方 |
|---|---|---|
| `briefing.submitted` | 提交简报 | 消息(通知评审人·P1)、活动流 |

> 说明：评审(briefing.reviewed)、跟进问题(briefing.issue.*)随 P1/P2 能力登记。

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
