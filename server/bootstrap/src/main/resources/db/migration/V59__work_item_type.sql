-- V59 阶段2 工作项类型 + 工作流引擎：可配置工作项类型(=字段集+工作流+模板)，
-- 工作流以「类型 × from_status × to_status」转移行定义，取代硬编码 TaskWorkflow。
-- 双轨：仅自用租户(tenant_id=1)种子「默认任务类型」及与现 TaskWorkflow 等价的流转矩阵；
-- 引擎对已配置租户生效，未配置租户回落 TaskWorkflow。task 接入 type_id/status_id 留阶段3。
-- 公共字段对齐其余业务表；tenant_id 由多租户拦截器注入。

CREATE TABLE pm_work_item_type (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  code VARCHAR(32),                          -- 类型编码(租户内唯一)
  name VARCHAR(32),
  group_name VARCHAR(32),                    -- 业务域分组(通用/IT/设计…)
  builtin TINYINT DEFAULT 0,                 -- 1=内置(默认任务类型,不可删)
  sort INT DEFAULT 0,
  status VARCHAR(16) DEFAULT 'active',
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_tenant(tenant_id), KEY idx_code(tenant_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作项类型';

CREATE TABLE pm_work_item_type_field (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  type_id BIGINT,
  field_key VARCHAR(64),                     -- 字段键(系统字段名或自定义 fieldKey)
  required TINYINT DEFAULT 0,
  sort INT DEFAULT 0,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_type(tenant_id, type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作项类型-字段绑定';

-- 注：表名取 pm_work_item_transition（按工作项类型组织），与 data-model.md 既有
-- pm_workflow_transition（旧 pm_workflow 串式占位，P1 可配工作流）区分，避免清库全量迁移撞名。
CREATE TABLE pm_work_item_transition (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  type_id BIGINT,
  from_status_id BIGINT,
  to_status_id BIGINT,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_wit(tenant_id, type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作项类型状态转移矩阵';

-- 补「已验收」状态(对齐 TaskStatus.ACCEPTED；归约到 已完成 元类别)
INSERT INTO pm_status
 (id, tenant_id, name, color, meta_category, group_name, sort, builtin, status, is_deleted) VALUES
 (4, 1, '已验收', 'success', '已完成', '通用', 40, 1, 'active', 0);

-- 默认任务类型(承接存量任务)
INSERT INTO pm_work_item_type
 (id, tenant_id, code, name, group_name, builtin, sort, status, is_deleted) VALUES
 (1, 1, 'task', '默认任务', '通用', 1, 10, 'active', 0);

-- 默认类型流转矩阵(等价于现 TaskWorkflow)：状态 id 对应 pm_status 1未开始/2进行中/3已完成/4已验收
INSERT INTO pm_work_item_transition
 (id, tenant_id, type_id, from_status_id, to_status_id, is_deleted) VALUES
 (1, 1, 1, 1, 2, 0),   -- 未开始 → 进行中
 (2, 1, 1, 2, 1, 0),   -- 进行中 → 未开始
 (3, 1, 1, 2, 3, 0),   -- 进行中 → 已完成
 (4, 1, 1, 3, 2, 0),   -- 已完成 → 进行中
 (5, 1, 1, 3, 4, 0),   -- 已完成 → 已验收
 (6, 1, 1, 4, 3, 0);   -- 已验收 → 已完成
