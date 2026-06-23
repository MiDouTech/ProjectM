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
  require_goal_alignment TINYINT DEFAULT 0,-- 立项是否强制已对齐目标(V19，S 类默认 1)
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
  metric_current DECIMAL(14,2), progress DECIMAL(5,2) DEFAULT 0,
  auto_rollup TINYINT DEFAULT 0,           -- V20：KR 进度是否自动汇总对齐项目任务完成率
  KEY idx_tenant(tenant_id)
);
CREATE TABLE pm_goal_alignment (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, goal_id BIGINT,
  target_type VARCHAR(16), target_id BIGINT,
  weight DECIMAL(5,2) DEFAULT 1,           -- V20：对齐贡献权重(多项目汇总到一个 KR 加权)
  KEY idx_goal(goal_id), KEY idx_target(target_type,target_id)
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
-- 开放平台 API Key（P2.2，租户业务表）：key 绑定用户、等同其身份调 OpenAPI；仅存 SHA-256 与前缀。
CREATE TABLE sys_api_key (id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
  name VARCHAR(64), key_hash CHAR(64), key_prefix VARCHAR(16), status VARCHAR(16) DEFAULT 'active',
  last_used_at DATETIME, expire_at DATETIME, UNIQUE KEY uk_apikey_hash(key_hash), KEY idx_apikey_tenant(tenant_id));

-- ========== 平台基础 ==========
CREATE TABLE sys_domain_event (id BIGINT PRIMARY KEY, tenant_id BIGINT, event_type VARCHAR(64), payload JSON, status VARCHAR(16) DEFAULT 'pending', create_time DATETIME, KEY idx_status(status));
CREATE TABLE sys_audit_log (id BIGINT PRIMARY KEY, tenant_id BIGINT, user_id BIGINT, action VARCHAR(64), target VARCHAR(64), detail JSON, create_time DATETIME);
CREATE TABLE ai_suggestion (id BIGINT PRIMARY KEY, tenant_id BIGINT, type VARCHAR(32), ref_type VARCHAR(16), ref_id BIGINT, content JSON, status VARCHAR(16) DEFAULT 'pending', create_time DATETIME);

-- ========== 平台域（SaaS 运营总后台，跨租户全局，【不带 tenant_id】，不参与多租户隔离）==========
-- 见 server/module-platform 与 CLAUDE.md §4「平台域」。公共字段同业务表但无 tenant_id；审计表为追加写无逻辑删除。
-- 实体继承 PlatformBaseEntity；所有表登记在 MidoTenantLineHandler 忽略名单。落地见 V27/V28 migration。
CREATE TABLE sys_tenant (                 -- 租户注册表：本表 id 即业务侧各表的 tenant_id
  id BIGINT PRIMARY KEY, code VARCHAR(32) NOT NULL, name VARCHAR(128) NOT NULL,
  status VARCHAR(16) DEFAULT 'trial',     -- trial/active/suspended/expired/closed
  industry VARCHAR(64), contact_name VARCHAR(64), contact_phone VARCHAR(32), contact_email VARCHAR(128),
  source VARCHAR(16) DEFAULT 'manual', admin_user_id BIGINT,  -- admin_user_id=租户主管理员 sys_user.id(P1)
  activated_at DATETIME, expire_at DATETIME,                  -- expire_at 随订阅，空=不限期
  UNIQUE KEY uk_tenant_code(code), KEY idx_status(status), KEY idx_expire(expire_at));
CREATE TABLE sys_plan (                    -- 套餐(price=线下参考价,不接支付网关)
  id BIGINT PRIMARY KEY, code VARCHAR(32) NOT NULL, name VARCHAR(64) NOT NULL,
  price DECIMAL(14,2), billing_cycle VARCHAR(16) DEFAULT 'yearly', -- monthly/yearly/once
  status VARCHAR(16) DEFAULT 'active', sort INT DEFAULT 0, remark VARCHAR(255), UNIQUE KEY uk_plan_code(code));
CREATE TABLE sys_plan_quota (             -- 套餐配额项(limit_value=-1 表示不限)
  id BIGINT PRIMARY KEY, plan_id BIGINT NOT NULL, resource VARCHAR(32) NOT NULL, -- user/project/storage_mb/task
  limit_value BIGINT DEFAULT -1, KEY idx_plan(plan_id));
CREATE TABLE sys_tenant_subscription (    -- 租户订阅(每租户至多一条 active；tenant_id 为普通引用列非隔离列)
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, plan_id BIGINT NOT NULL,
  start_at DATETIME, expire_at DATETIME, status VARCHAR(16) DEFAULT 'active', -- active/expired/cancelled
  quota_override TEXT, remark VARCHAR(255), KEY idx_tenant(tenant_id,status), KEY idx_plan(plan_id));
CREATE TABLE sys_platform_admin (         -- 平台运营账号(独立账号体系,不属于任何租户)
  id BIGINT PRIMARY KEY, username VARCHAR(64) NOT NULL, password VARCHAR(128) NOT NULL, name VARCHAR(64) NOT NULL,
  status VARCHAR(16) DEFAULT 'active', last_login_at DATETIME, UNIQUE KEY uk_username(username));
CREATE TABLE sys_platform_role (id BIGINT PRIMARY KEY, name VARCHAR(64), code VARCHAR(64) NOT NULL, remark VARCHAR(255), UNIQUE KEY uk_code(code));
CREATE TABLE sys_platform_admin_role (id BIGINT PRIMARY KEY, admin_id BIGINT, role_id BIGINT, KEY idx_admin(admin_id));
CREATE TABLE sys_platform_role_perm (id BIGINT PRIMARY KEY, role_id BIGINT, perm_code VARCHAR(64), KEY idx_role(role_id)); -- perm_code 取自 PlatformPerms
CREATE TABLE sys_platform_audit_log (     -- 平台运营审计(追加写,无逻辑删除)
  id BIGINT PRIMARY KEY, admin_id BIGINT, action VARCHAR(64), target VARCHAR(32), target_id BIGINT,
  detail TEXT, ip VARCHAR(64), create_time DATETIME, KEY idx_target(target,target_id));
CREATE TABLE sys_tenant_quota_usage (     -- 租户用量快照(P1)，每(tenant,resource)一行，定时任务 upsert
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, resource VARCHAR(32) NOT NULL, -- user/project/task/storage_mb
  used_value BIGINT DEFAULT 0, snapshot_time DATETIME, UNIQUE KEY uk_usage_tenant_res(tenant_id, resource));
-- P2.1 平台域追加表
CREATE TABLE sys_revenue_record (         -- 线下收入台账(type=payment 收款/refund 退款)
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, type VARCHAR(16) DEFAULT 'payment',
  amount DECIMAL(14,2) NOT NULL, contract_no VARCHAR(64), occurred_date DATE, remark VARCHAR(512),
  KEY idx_rev_tenant(tenant_id));
CREATE TABLE sys_announcement (           -- 平台公告(status=draft/published, level=info/warning)
  id BIGINT PRIMARY KEY, title VARCHAR(256) NOT NULL, content TEXT NOT NULL, level VARCHAR(16) DEFAULT 'info',
  status VARCHAR(16) DEFAULT 'draft', publish_at DATETIME, expire_at DATETIME, KEY idx_ann_status(status));
CREATE TABLE sys_plan_feature (           -- 套餐功能开关(feature_code 取自 FeatureCodes)
  id BIGINT PRIMARY KEY, plan_id BIGINT NOT NULL, feature_code VARCHAR(32) NOT NULL,
  enabled TINYINT DEFAULT 1, KEY idx_pf_plan(plan_id));
-- P2.2 注销合规 + 数据导出
-- sys_tenant 追加列 purge_scheduled_at DATETIME(注销清除计划时间)。
CREATE TABLE sys_tenant_export (          -- 租户数据导出任务(异步: pending→processing→done/failed)
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, status VARCHAR(16) DEFAULT 'pending',
  file_key VARCHAR(512), error VARCHAR(512), requested_by BIGINT,
  KEY idx_export_tenant(tenant_id), KEY idx_export_status(status));
```

> P1 多租户登录隔离：`sys_user` 唯一约束由【全局唯一手机号 uk_user_phone】改为【租户内唯一 uk_user_tenant_phone(tenant_id, phone)】（V29）。
> 登录令牌携带租户声明（tid），登录按租户编码 + 账号定位用户；模拟登录令牌额外携带 imp（发起运营账号）声明。

## 日历/日程域（calendar.*，V38）

> 独立「事件型日程」域，区别于任务日历视图（`pm_view` type=calendar 按截止日渲染任务），二者不共表。
> 日历叠加任务截止/里程碑由前端聚合读取（P1），本域不复制 `pm_task`。落地见 V38 migration。

```sql
CREATE TABLE pm_calendar (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL, type VARCHAR(16) DEFAULT 'personal',  -- personal/meeting/team/resource
  owner_id BIGINT, color VARCHAR(16), visibility VARCHAR(16) DEFAULT 'private', -- private/busy/public
  is_default TINYINT DEFAULT 0,            -- 用户默认「我的日程」(每用户至多一个)
  status VARCHAR(16) DEFAULT 'active',     -- active/archived
  -- + 公共字段
  KEY idx_owner(tenant_id, owner_id));
CREATE TABLE pm_schedule (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, calendar_id BIGINT NOT NULL,
  title VARCHAR(256) NOT NULL, description TEXT,
  start_time DATETIME NOT NULL, end_time DATETIME NOT NULL, all_day TINYINT DEFAULT 0,
  location VARCHAR(256), recur_rule VARCHAR(512), reminder JSON,  -- recur_rule/reminder 预留(P1)
  allow_feedback TINYINT DEFAULT 1,        -- 是否允许参与人 RSVP
  source_type VARCHAR(16) DEFAULT 'manual',-- manual/task/meeting
  source_id BIGINT, organizer_id BIGINT, status VARCHAR(16) DEFAULT 'confirmed', -- confirmed/cancelled
  -- + 公共字段
  KEY idx_cal(calendar_id), KEY idx_range(tenant_id, start_time, end_time));
CREATE TABLE pm_schedule_participant (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, schedule_id BIGINT NOT NULL,
  user_id BIGINT, external_name VARCHAR(128),
  role VARCHAR(16) DEFAULT 'required',     -- organizer/required/optional
  rsvp_status VARCHAR(16) DEFAULT 'pending', -- pending/accepted/tentative/declined
  -- + 公共字段
  KEY idx_sch(schedule_id), KEY idx_user(tenant_id, user_id));
-- 资源 + 占用（V39）：会议室/设备与日程占用，用于冲突检测(同资源时间重叠即冲突)。
CREATE TABLE pm_calendar_resource (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL, type VARCHAR(16) DEFAULT 'room', -- room/device
  capacity INT, location VARCHAR(256), status VARCHAR(16) DEFAULT 'active', -- active/disabled
  -- + 公共字段
  KEY idx_tenant(tenant_id));
CREATE TABLE pm_schedule_resource (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, schedule_id BIGINT NOT NULL, resource_id BIGINT NOT NULL,
  -- + 公共字段
  KEY idx_sch(schedule_id), KEY idx_res(resource_id));
```

## 状态字典（枚举，集中维护，禁散落魔法值）
- 项目状态：`草稿 / 审批中 / 已注册 / 进行中 / 结果验收 / 已结案 / 价值验收中 / 已评价`
- 任务状态（默认流，可工作流自定义）：`未开始 / 进行中 / 已完成 / 已验收`
- 审批实例：`pending / approved / rejected`；审批动作：`approve / reject / transfer`
- NPSS result_level：`success / mixed / failure`
- 通知 channel：`inapp / wecom`
- 日历类型 type：`personal / meeting / team / resource`；日程 status：`confirmed / cancelled`；RSVP：`pending / accepted / tentative / declined`
- 数据范围 scope：`self / dept / dept_and_sub / all / custom`
- 租户状态（平台域）：`trial 试用 / active 正式 / suspended 停用 / expired 已过期 / closed 已注销`
- 套餐/平台账号状态（平台域）：`active 启用 / disabled 停用`；订阅状态：`active / expired / cancelled`
