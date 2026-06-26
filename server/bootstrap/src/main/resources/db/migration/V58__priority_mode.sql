-- V58 阶段1-c 优先级模式：租户可配置的优先级档位集（对标 Worktile「优先级模式」）。
-- 双轨：本表先建+可配置，task 仍走前端固定 TASK_PRIORITIES；阶段3 工作项接入后切换。
-- 种子「默认优先级模式」档位对齐现有 高/中/低(1/2/3)，便于后续平滑迁移。
-- 公共字段对齐其余业务表；tenant_id 由多租户拦截器注入（种子按自用租户 tenant_id=1）。

CREATE TABLE pm_priority_mode (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  name VARCHAR(32),
  remark VARCHAR(255),
  builtin TINYINT DEFAULT 0,
  status VARCHAR(16) DEFAULT 'active',
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_tenant(tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优先级模式';

CREATE TABLE pm_priority_level (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  mode_id BIGINT,
  name VARCHAR(32),
  color VARCHAR(16),
  level_value INT,                           -- 档位值(越小越高，对齐旧 1高/2中/3低)
  sort INT DEFAULT 0,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_mode(tenant_id, mode_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优先级档位';

INSERT INTO pm_priority_mode (id, tenant_id, name, remark, builtin, status, is_deleted) VALUES
 (1, 1, '默认优先级模式', '高/中/低三档', 1, 'active', 0);

INSERT INTO pm_priority_level (id, tenant_id, mode_id, name, color, level_value, sort, is_deleted) VALUES
 (1, 1, 1, '高', 'danger',  1, 10, 0),
 (2, 1, 1, '中', 'warning', 2, 20, 0),
 (3, 1, 1, '低', 'info',    3, 30, 0);
