-- V61 阶段4 关联关系：工作项之间的「相关(related)/派生(derived)」关联，支撑 需求-任务-缺陷 追溯链。
-- relation_def 定义"哪种类型可关联哪种类型、关系语义"；relation 为实例级关联(源工作项→目标工作项)。
-- 公共字段对齐其余业务表；tenant_id 由多租户拦截器注入。

CREATE TABLE pm_relation_def (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  source_type_id BIGINT,                     -- 源工作项类型 → pm_work_item_type
  target_type_id BIGINT,                     -- 目标工作项类型
  relation_kind VARCHAR(16),                 -- related 相关(横向) / derived 派生(纵向父子)
  name VARCHAR(32),                          -- 关系显示名(如 "产生的缺陷")
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_tenant(tenant_id), KEY idx_src(tenant_id, source_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作项关联定义';

CREATE TABLE pm_relation (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  relation_kind VARCHAR(16),                 -- related / derived
  source_task_id BIGINT,                     -- 源工作项(任务) → pm_task
  target_task_id BIGINT,                     -- 目标工作项(任务)
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_src(tenant_id, source_task_id), KEY idx_tgt(tenant_id, target_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作项关联(实例)';
