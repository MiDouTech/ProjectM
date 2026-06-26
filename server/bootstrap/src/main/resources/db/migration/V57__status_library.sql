-- V57 阶段1-b 状态库：租户可配置的任务状态字典（对标 Worktile「状态」）。
-- 每状态归约到 3 个「元类别」(未开始/进行中/已完成)，保证跨项目统计口径统一。
-- 双轨：本表先建+可配置，task 仍走 TaskStatus enum；阶段2 工作流引擎接入后再切换。
-- 公共字段对齐其余业务表；tenant_id 由多租户拦截器注入（种子按自用租户 tenant_id=1）。

CREATE TABLE pm_status (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  name VARCHAR(32),
  color VARCHAR(16),                         -- design token 短名(如 info/primary/success)
  meta_category VARCHAR(16),                 -- 元类别：未开始/进行中/已完成
  group_name VARCHAR(32),                    -- 分组(通用/需求/缺陷…)
  sort INT DEFAULT 0,
  builtin TINYINT DEFAULT 0,                 -- 1=内置(不可删)
  status VARCHAR(16) DEFAULT 'active',       -- active/disabled
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_tenant(tenant_id), KEY idx_meta(tenant_id, meta_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='状态库(任务状态字典)';

INSERT INTO pm_status
 (id, tenant_id, name, color, meta_category, group_name, sort, builtin, status, is_deleted) VALUES
 (1, 1, '未开始', 'info',    '未开始', '通用', 10, 1, 'active', 0),
 (2, 1, '进行中', 'primary', '进行中', '通用', 20, 1, 'active', 0),
 (3, 1, '已完成', 'success', '已完成', '通用', 30, 1, 'active', 0);
