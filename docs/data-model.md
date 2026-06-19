# 数据模型 / 完整 DDL（事实源）

> 以本文件为准生成 Entity/Mapper。方言 MySQL 8.0。所有表追加公共字段（下方），主键用雪花 ID（BIGINT）。逻辑删除 `is_deleted`。多租户 `tenant_id` 由拦截器注入，业务代码不手写。

## 公共字段（每张业务表都有，下方 DDL 省略不重复书写）
```sql
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0
```

## 设计决策（不可擅改）
- `plan` 不单独建实体：计划 = 任务沿时间轴排布。
- `milestone` 不单独建实体：作为 `pm_task.is_milestone` 标记位。
- 目标(Goal/KR)不挂执行树，与项目/任务通过 `pm_goal_alignment` 弱关联（多对多）。
- 同一份任务数据多视图渲染，靠 `pm_view` 配置，不建多套表。

---

```sql
-- ========== 项目域 ==========
CREATE TABLE pm_project (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  code VARCHAR(64), name VARCHAR(128) NOT NULL, description TEXT,
  category VARCHAR(8) NOT NULL,            -- S/I/O
  sub_category VARCHAR(16),                -- 常规运营/定向整改/专项督办
  template_id BIGINT, leader_id BIGINT,
  dept_id BIGINT,                          -- V9 追加：归属部门(=leader 部门)，数据范围按部门过滤
  status VARCHAR(32), workflow_id BIGINT,
  start_date DATE, end_date DATE,
  budget DECIMAL(14,2), actual_cost DECIMAL(14,2),
  value_review_due_date DATE,
  requires_npss TINYINT DEFAULT 1,         -- V14 追加：是否走NPSS价值验收 1是/0否（仅O·定向整改/专项督办默认0）；非NPSS项目结案即终止
  pmo_registered_at DATETIME, archived TINYINT DEFAULT 0,
  -- + 公共字段
  KEY idx_tenant(tenant_id), KEY idx_cat(category), KEY idx_review(value_review_due_date)
);
CREATE TABLE pm_project_member (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, project_id BIGINT, user_id BIGINT,
  project_role VARCHAR(32),                -- 管理员/普通成员/只读成员
  KEY idx_proj(project_id)
);
CREATE TABLE pm_project_template (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(64), category VARCHAR(8),
  sub_category VARCHAR(16), description TEXT, is_builtin TINYINT DEFAULT 0,
  config JSON                              -- 阶段/任务骨架/默认干系人权重/默认审批流/默认字段
);
-- 项目类型注册表（V17）：SaaS 租户自配，取代硬编码枚举 S/I/O。把立项职级门槛/是否走NPSS/
-- 默认审批流/干系人权重模板收敛为类型属性。扁平建模（O 三子类各一条），parent_code 供报表汇总。
CREATE TABLE pm_project_type (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  code VARCHAR(32) NOT NULL,               -- 租户内唯一程序引用（S/I/O_NORMAL...）
  name VARCHAR(64) NOT NULL, parent_code VARCHAR(32),  -- parent_code 报表汇总 O_*→O
  color VARCHAR(16), icon VARCHAR(32), sort INT DEFAULT 0,
  min_job_level VARCHAR(8),                -- 立项 Leader 最低职级门槛（空=不限）
  requires_npss TINYINT DEFAULT 1,         -- 默认是否走 NPSS
  default_flow_id BIGINT,                  -- 绑定默认审批流（approval_flow.id）
  stakeholder_tpl JSON,                    -- 默认干系人权重模板
  status VARCHAR(16) DEFAULT 'active',     -- active/disabled
  description VARCHAR(255), KEY idx_tenant(tenant_id), KEY idx_code(tenant_id,code)
);

-- ========== 任务域 ==========
CREATE TABLE pm_task (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, project_id BIGINT NOT NULL,
  parent_id BIGINT DEFAULT 0, title VARCHAR(256) NOT NULL, description TEXT,
  assignee_id BIGINT, dept_id BIGINT,  -- dept_id V9 追加：=所属项目部门，数据范围按部门过滤
  status VARCHAR(32), priority TINYINT, stage VARCHAR(32),
  start_date DATE, due_date DATE, is_milestone TINYINT DEFAULT 0, recur_rule JSON,
  est_hours DECIMAL(8,2), actual_hours DECIMAL(8,2),
  custom_fields JSON, ai_source VARCHAR(32) DEFAULT 'human',
  KEY idx_proj(project_id), KEY idx_assignee(assignee_id), KEY idx_due(due_date)
);
CREATE TABLE pm_task_dependency (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, predecessor_id BIGINT, successor_id BIGINT,
  type VARCHAR(8) DEFAULT 'FS', KEY idx_pre(predecessor_id), KEY idx_suc(successor_id)
);
CREATE TABLE pm_work_hour (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, task_id BIGINT, user_id BIGINT,
  kind VARCHAR(8), category VARCHAR(16), work_date DATE, hours DECIMAL(8,2), remark VARCHAR(200),
  KEY idx_task(task_id)
);

-- ========== 目标域 ==========
CREATE TABLE pm_goal (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, title VARCHAR(256), type VARCHAR(16),
  parent_id BIGINT DEFAULT 0, owner_id BIGINT, period VARCHAR(32),
  metric_unit VARCHAR(16), metric_start DECIMAL(14,2), metric_target DECIMAL(14,2),
  metric_current DECIMAL(14,2), progress DECIMAL(5,2) DEFAULT 0, KEY idx_tenant(tenant_id)
);
CREATE TABLE pm_goal_alignment (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, goal_id BIGINT,
  target_type VARCHAR(16), target_id BIGINT, KEY idx_goal(goal_id), KEY idx_target(target_type,target_id)
);

-- ========== 干系人 + NPSS 验收域 ==========
CREATE TABLE pm_stakeholder (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, project_id BIGINT NOT NULL,
  user_id BIGINT, external_name VARCHAR(128),
  role VARCHAR(32), category VARCHAR(16), power_level TINYINT, interest_level TINYINT,
  npss_weight DECIMAL(5,2), KEY idx_proj(project_id)
);
CREATE TABLE pm_npss_review (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, project_id BIGINT NOT NULL,
  round VARCHAR(32), status VARCHAR(16), weighted_score DECIMAL(5,2),
  result_level VARCHAR(16), reviewed_at DATETIME, KEY idx_proj(project_id)
);
CREATE TABLE pm_npss_score (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, review_id BIGINT NOT NULL,
  stakeholder_id BIGINT NOT NULL, score TINYINT, weight DECIMAL(5,2), comment TEXT,
  KEY idx_review(review_id)
);

-- ========== 立项/审批引擎 ==========
CREATE TABLE approval_form (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(64), biz_type VARCHAR(32), schema JSON);
CREATE TABLE approval_flow (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(64), biz_type VARCHAR(32),
  mode VARCHAR(16), definition JSON);
  -- definition.nodes[]：key/name/mode(or|and 会签或签)/guard/cc/condition(条件分支) +
  --   approverType(USER|ROLE|DEPT_HEAD|DIRECT_LEADER|APPLICANT_SELF, 缺省 USER) + approverValues(角色ID/层级)。
  -- 类型→流程绑定的事实源为 pm_project_type.default_flow_id（项目类型调用审批流）。
CREATE TABLE approval_instance (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, flow_id BIGINT, biz_type VARCHAR(32),
  biz_id BIGINT, status VARCHAR(16), current_node VARCHAR(64), form_data JSON, applicant_id BIGINT,
  KEY idx_biz(biz_type,biz_id));
CREATE TABLE approval_task (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, instance_id BIGINT, node VARCHAR(64),
  approver_id BIGINT, action VARCHAR(16), comment TEXT, acted_at DATETIME,
  KEY idx_inst(instance_id), KEY idx_approver(approver_id));

-- ========== 费用域（预算绑定）==========
CREATE TABLE pm_cost (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, project_id BIGINT, title VARCHAR(128),
  account VARCHAR(32), budget_amount DECIMAL(14,2), actual_amount DECIMAL(14,2),
  occur_date DATE, pay_date DATE, status VARCHAR(16), approval_id BIGINT, KEY idx_proj(project_id));

-- ========== 工作流（可配置状态流）==========
CREATE TABLE pm_workflow (id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(64), apply_to VARCHAR(16), category VARCHAR(8));
CREATE TABLE pm_workflow_status (id BIGINT PRIMARY KEY, tenant_id BIGINT, workflow_id BIGINT, code VARCHAR(32), name VARCHAR(32), category VARCHAR(16), sort INT);
CREATE TABLE pm_workflow_transition (id BIGINT PRIMARY KEY, tenant_id BIGINT, workflow_id BIGINT, from_status VARCHAR(32), to_status VARCHAR(32), guard_role VARCHAR(64));

-- ========== 视图配置 ==========
CREATE TABLE pm_view (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, scope VARCHAR(16), owner_id BIGINT,
  type VARCHAR(16), name VARCHAR(64), project_id BIGINT, config JSON);
-- name/project_id 由 V8 追加（视图设计器命名与项目级绑定）；scope=personal|project|workbench。
-- config 仅承载查询配置(锁定 schema)：{groupBy, sort:[{field,dir}], expandLevel(1-5),
--   filters:{logic:and|or, conditions:[{field,op,value}]}, columns:[field]}。详见 ViewConfig。

-- ========== 自定义字段 EAV ==========
CREATE TABLE pm_field_def (id BIGINT PRIMARY KEY, tenant_id BIGINT, scope VARCHAR(16), name VARCHAR(64), type VARCHAR(32));
CREATE TABLE pm_field_value (id BIGINT PRIMARY KEY, tenant_id BIGINT, field_id BIGINT, entity_type VARCHAR(16), entity_id BIGINT, value TEXT, KEY idx_entity(entity_type,entity_id));

-- ========== 协作域 ==========
CREATE TABLE pm_comment (id BIGINT PRIMARY KEY, tenant_id BIGINT, entity_type VARCHAR(16), entity_id BIGINT, user_id BIGINT, content TEXT, mention JSON, KEY idx_entity(entity_type,entity_id));
CREATE TABLE pm_notification (id BIGINT PRIMARY KEY, tenant_id BIGINT, user_id BIGINT, type VARCHAR(32), title VARCHAR(128), payload JSON, is_read TINYINT DEFAULT 0, channel VARCHAR(16), KEY idx_user(user_id,is_read));
CREATE TABLE pm_attachment (id BIGINT PRIMARY KEY, tenant_id BIGINT, entity_type VARCHAR(16), entity_id BIGINT, name VARCHAR(256), oss_key VARCHAR(512), size BIGINT, KEY idx_entity(entity_type,entity_id));

-- ========== 组织/权限域 ==========
CREATE TABLE sys_user (id BIGINT PRIMARY KEY, tenant_id BIGINT, username VARCHAR(64), phone VARCHAR(20), name VARCHAR(64), password VARCHAR(128), dept_id BIGINT, job_level VARCHAR(8), status VARCHAR(16), KEY idx_uname(username), UNIQUE KEY uk_user_phone(phone)); -- phone=登录账号(全局唯一,见 V11)；手机号/用户名双登录
CREATE TABLE sys_dept (id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(64), parent_id BIGINT DEFAULT 0,
  leader_id BIGINT, KEY idx_parent(parent_id));  -- leader_id(V18)=部门负责人，动态审批人「部门主管/直属上级」解析用
CREATE TABLE sys_role (id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(64), code VARCHAR(64));
CREATE TABLE sys_user_role (id BIGINT PRIMARY KEY, tenant_id BIGINT, user_id BIGINT, role_id BIGINT, KEY idx_user(user_id));
CREATE TABLE sys_role_perm (id BIGINT PRIMARY KEY, tenant_id BIGINT, role_id BIGINT, perm_code VARCHAR(64), KEY idx_role(role_id));
CREATE TABLE sys_role_data_scope (id BIGINT PRIMARY KEY, tenant_id BIGINT, role_id BIGINT, resource VARCHAR(32), scope VARCHAR(16)); -- self/dept/dept_and_sub/all/custom
CREATE TABLE sys_identity_map (id BIGINT PRIMARY KEY, tenant_id BIGINT, user_id BIGINT, provider VARCHAR(16), external_id VARCHAR(128), KEY idx_ext(provider,external_id));

-- ========== 平台基础 ==========
CREATE TABLE sys_domain_event (id BIGINT PRIMARY KEY, tenant_id BIGINT, event_type VARCHAR(64), payload JSON, status VARCHAR(16) DEFAULT 'pending', create_time DATETIME, KEY idx_status(status));
CREATE TABLE sys_audit_log (id BIGINT PRIMARY KEY, tenant_id BIGINT, user_id BIGINT, action VARCHAR(64), target VARCHAR(64), detail JSON, create_time DATETIME);
CREATE TABLE ai_suggestion (id BIGINT PRIMARY KEY, tenant_id BIGINT, type VARCHAR(32), ref_type VARCHAR(16), ref_id BIGINT, content JSON, status VARCHAR(16) DEFAULT 'pending', create_time DATETIME);
```

## 状态字典（枚举，集中维护，禁散落魔法值）
- 项目状态：`草稿 / 审批中 / 已注册 / 进行中 / 结果验收 / 已结案 / 价值验收中 / 已评价`
- 任务状态（默认流，可工作流自定义）：`未开始 / 进行中 / 已完成 / 已验收`
- 审批实例：`pending / approved / rejected`；审批动作：`approve / reject / transfer`
- NPSS result_level：`success / mixed / failure`
- 通知 channel：`inapp / wecom`
- 数据范围 scope：`self / dept / dept_and_sub / all / custom`
