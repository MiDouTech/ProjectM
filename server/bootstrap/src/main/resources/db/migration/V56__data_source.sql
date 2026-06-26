-- V56 阶段1-a 数据源/选项集库：可复用的下拉选项集（对标 Worktile「数据源」）。
-- 下拉字段(select/multi_select)可引用数据源获得选项，集中维护复用，取代内联 options 的重复。
-- 双轨：pm_field_def 同时保留内联 options 与新增 data_source_id；引用数据源时 data_source_id 优先。
-- 公共字段对齐其余业务表；tenant_id 由多租户拦截器注入。

CREATE TABLE pm_data_source (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  name VARCHAR(64),
  group_name VARCHAR(32),                    -- 分组（如 IT/招聘/项目）
  remark VARCHAR(255),
  status VARCHAR(16) DEFAULT 'active',       -- active/disabled
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_tenant(tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源（可复用选项集）';

CREATE TABLE pm_data_source_option (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  data_source_id BIGINT,
  value VARCHAR(64),                         -- 入库值
  label VARCHAR(128),                        -- 显示文案
  sort_no INT DEFAULT 0,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_ds(tenant_id, data_source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源选项';

-- 字段定义引用数据源（下拉字段二选一：data_source_id 优先，否则用内联 options）
ALTER TABLE pm_field_def
  ADD COLUMN data_source_id BIGINT NULL COMMENT '引用的数据源 ID（select/multi_select 用，优先于内联 options）' AFTER options;
