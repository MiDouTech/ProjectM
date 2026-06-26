-- =====================================================================
-- V1 初始化库表结构
-- 唯一事实源：docs/data-model.md（方言 MySQL 8.0）。本文件逐字落地该 DDL，
-- 不自创表、不改字段。业务表统一补回文档「公共字段」（文档为避免重复而省略书写）：
--   create_by / create_time / update_by / update_time / is_deleted
-- 平台基础表（sys_domain_event / sys_audit_log / ai_suggestion）按文档仅保留各自列。
-- 主键雪花 BIGINT；tenant_id 由多租户拦截器注入；is_deleted 逻辑删除。
--
-- 状态字典（docs/data-model.md，禁散落魔法值）：
--   项目状态：草稿 / 审批中 / 已注册 / 进行中 / 结果验收 / 已结案 / 价值验收中 / 已评价
--   任务状态：未开始 / 进行中 / 已完成 / 已验收
--   审批实例：pending / approved / rejected；审批动作：approve / reject / transfer
--   NPSS result_level：success / mixed / failure
--   通知 channel：inapp / wecom
--   数据范围 scope：self / dept / dept_and_sub / all / custom
-- =====================================================================

-- ========== 项目域 ==========
CREATE TABLE pm_project (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  code VARCHAR(64), name VARCHAR(128) NOT NULL, description TEXT,
  category VARCHAR(8) NOT NULL,            -- S/I/O
  sub_category VARCHAR(16),                -- 常规运营/定向整改/专项督办
  template_id BIGINT, leader_id BIGINT,
  status VARCHAR(32), workflow_id BIGINT,
  start_date DATE, end_date DATE,
  budget DECIMAL(14,2), actual_cost DECIMAL(14,2),
  value_review_due_date DATE, pmo_registered_at DATETIME, archived TINYINT DEFAULT 0,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_tenant(tenant_id), KEY idx_cat(category), KEY idx_review(value_review_due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目';

CREATE TABLE pm_project_member (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, project_id BIGINT, user_id BIGINT,
  project_role VARCHAR(32),                -- 管理员/普通成员/只读成员
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_proj(project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员';

CREATE TABLE pm_project_template (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(64), category VARCHAR(8),
  sub_category VARCHAR(16), description TEXT, is_builtin TINYINT DEFAULT 0,
  config JSON,                             -- 阶段/任务骨架/默认干系人权重/默认审批流/默认字段
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目模板';

-- ========== 任务域 ==========
CREATE TABLE pm_task (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, project_id BIGINT NOT NULL,
  parent_id BIGINT DEFAULT 0, title VARCHAR(256) NOT NULL, description TEXT,
  assignee_id BIGINT, status VARCHAR(32), priority TINYINT, stage VARCHAR(32),
  start_date DATE, due_date DATE, is_milestone TINYINT DEFAULT 0, recur_rule JSON,
  est_hours DECIMAL(8,2), actual_hours DECIMAL(8,2),
  custom_fields JSON, ai_source VARCHAR(32) DEFAULT 'human',
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_proj(project_id), KEY idx_assignee(assignee_id), KEY idx_due(due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务';

CREATE TABLE pm_task_dependency (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, predecessor_id BIGINT, successor_id BIGINT,
  type VARCHAR(8) DEFAULT 'FS',
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_pre(predecessor_id), KEY idx_suc(successor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务依赖';

CREATE TABLE pm_work_hour (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, task_id BIGINT, user_id BIGINT,
  kind VARCHAR(8), category VARCHAR(16), work_date DATE, hours DECIMAL(8,2), remark VARCHAR(200),
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_task(task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工时';

-- ========== 目标域 ==========
CREATE TABLE pm_goal (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, title VARCHAR(256), type VARCHAR(16),
  parent_id BIGINT DEFAULT 0, owner_id BIGINT, period VARCHAR(32),
  metric_unit VARCHAR(16), metric_start DECIMAL(14,2), metric_target DECIMAL(14,2),
  metric_current DECIMAL(14,2), progress DECIMAL(5,2) DEFAULT 0,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_tenant(tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目标/KR';

CREATE TABLE pm_goal_alignment (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, goal_id BIGINT,
  target_type VARCHAR(16), target_id BIGINT,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_goal(goal_id), KEY idx_target(target_type,target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目标对齐(弱关联)';

-- ========== 干系人 + NPSS 验收域 ==========
CREATE TABLE pm_stakeholder (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, project_id BIGINT NOT NULL,
  user_id BIGINT, external_name VARCHAR(128),
  role VARCHAR(32), category VARCHAR(16), power_level TINYINT, interest_level TINYINT,
  npss_weight DECIMAL(5,2),
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_proj(project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='干系人';

CREATE TABLE pm_npss_review (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, project_id BIGINT NOT NULL,
  round VARCHAR(32), status VARCHAR(16), weighted_score DECIMAL(5,2),
  result_level VARCHAR(16),                -- success / mixed / failure
  reviewed_at DATETIME,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_proj(project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NPSS 评分轮次';

CREATE TABLE pm_npss_score (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, review_id BIGINT NOT NULL,
  stakeholder_id BIGINT NOT NULL, score TINYINT, weight DECIMAL(5,2), comment TEXT,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_review(review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NPSS 干系人评分';

-- ========== 立项/审批引擎 ==========
CREATE TABLE approval_form (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(64), biz_type VARCHAR(32), `schema` JSON,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批表单';

CREATE TABLE approval_flow (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(64), biz_type VARCHAR(32),
  mode VARCHAR(16), definition JSON,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流定义';

CREATE TABLE approval_instance (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, flow_id BIGINT, biz_type VARCHAR(32),
  biz_id BIGINT, status VARCHAR(16),       -- pending / approved / rejected
  current_node VARCHAR(64), form_data JSON, applicant_id BIGINT,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_biz(biz_type,biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批实例';

CREATE TABLE approval_task (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, instance_id BIGINT, node VARCHAR(64),
  approver_id BIGINT, action VARCHAR(16),  -- approve / reject / transfer
  comment TEXT, acted_at DATETIME,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_inst(instance_id), KEY idx_approver(approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批任务';

-- ========== 费用域（预算绑定）==========
CREATE TABLE pm_cost (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, project_id BIGINT, title VARCHAR(128),
  account VARCHAR(32), budget_amount DECIMAL(14,2), actual_amount DECIMAL(14,2),
  occur_date DATE, pay_date DATE, status VARCHAR(16), approval_id BIGINT,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_proj(project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用';

-- ========== 工作流（可配置状态流）==========
CREATE TABLE pm_workflow (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(64), apply_to VARCHAR(16), category VARCHAR(8),
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流';

CREATE TABLE pm_workflow_status (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, workflow_id BIGINT, code VARCHAR(32), name VARCHAR(32), category VARCHAR(16), sort INT,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流状态';

CREATE TABLE pm_workflow_transition (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, workflow_id BIGINT, from_status VARCHAR(32), to_status VARCHAR(32), guard_role VARCHAR(64),
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流流转';

-- ========== 视图配置 ==========
CREATE TABLE pm_view (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, scope VARCHAR(16), owner_id BIGINT,
  type VARCHAR(16), config JSON,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视图配置';

-- ========== 自定义字段 EAV ==========
-- pm_field_def / pm_field_value 由 V47__custom_field.sql 创建（富版：field_key/options/required/...）。
-- 此处不再建表：原 V1 简版与 V47 撞名，会导致空库从零全量迁移在 V47 报 "table already exists"。
-- 现有库 V1 早已 applied 不会重跑，仅 checksum 变化（如启用严格校验需一次 flyway repair）。

-- ========== 协作域 ==========
CREATE TABLE pm_comment (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, entity_type VARCHAR(16), entity_id BIGINT, user_id BIGINT, content TEXT, mention JSON,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_entity(entity_type,entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论';

CREATE TABLE pm_notification (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, user_id BIGINT, type VARCHAR(32), title VARCHAR(128), payload JSON, is_read TINYINT DEFAULT 0,
  channel VARCHAR(16),                     -- inapp / wecom
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_user(user_id,is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知';

CREATE TABLE pm_attachment (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, entity_type VARCHAR(16), entity_id BIGINT, name VARCHAR(256), oss_key VARCHAR(512), size BIGINT,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_entity(entity_type,entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件';

-- ========== 组织/权限域 ==========
CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, username VARCHAR(64), name VARCHAR(64), password VARCHAR(128), dept_id BIGINT, job_level VARCHAR(8), status VARCHAR(16),
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_uname(username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

CREATE TABLE sys_dept (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(64), parent_id BIGINT DEFAULT 0,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_parent(parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门';

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(64), code VARCHAR(64),
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE sys_user_role (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, user_id BIGINT, role_id BIGINT,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色';

CREATE TABLE sys_role_perm (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, role_id BIGINT, perm_code VARCHAR(64),
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_role(role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限码';

CREATE TABLE sys_role_data_scope (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, role_id BIGINT, resource VARCHAR(32),
  scope VARCHAR(16),                       -- self/dept/dept_and_sub/all/custom
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色数据范围';

CREATE TABLE sys_identity_map (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, user_id BIGINT, provider VARCHAR(16), external_id VARCHAR(128),
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_ext(provider,external_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='身份映射(企微预留)';

-- ========== 平台基础（按文档仅含各自列，不套用公共字段）==========
CREATE TABLE sys_domain_event (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, event_type VARCHAR(64), payload JSON,
  status VARCHAR(16) DEFAULT 'pending', create_time DATETIME,
  KEY idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领域事件 Outbox';

CREATE TABLE sys_audit_log (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, user_id BIGINT, action VARCHAR(64), target VARCHAR(64), detail JSON, create_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志';

CREATE TABLE ai_suggestion (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, type VARCHAR(32), ref_type VARCHAR(16), ref_id BIGINT, content JSON,
  status VARCHAR(16) DEFAULT 'pending', create_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 建议(默认不启用)';
