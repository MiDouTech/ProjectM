-- V62 阶段5 组件化视图：组件库(catalog) + 项目已安装组件(实例)。项目顶栏由已安装组件动态生成。
-- 双轨/兼容：项目无安装记录时前端回落到默认全量 Tab（行为不变）；安装后按已装组件渲染与排序。
-- 组件 code 对齐现有项目工作区 Tab，安装即映射到既有面板，零重写。
-- 公共字段对齐其余业务表；tenant_id 由多租户拦截器注入（catalog 种子按自用租户 tenant_id=1）。

CREATE TABLE pm_component (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  code VARCHAR(32),                          -- 组件编码(对齐工作区 Tab)
  name VARCHAR(32),
  icon VARCHAR(32),                          -- Element Plus 图标名
  multi_instance TINYINT DEFAULT 0,          -- 1=可多实例(如多块看板)
  builtin TINYINT DEFAULT 0,
  sort INT DEFAULT 0,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_tenant(tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组件库(catalog)';

CREATE TABLE pm_project_component (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  project_id BIGINT,
  component_code VARCHAR(32),
  name VARCHAR(32),                          -- 实例显示名(多实例可不同)
  sort INT DEFAULT 0,
  enabled TINYINT DEFAULT 1,
  config TEXT,                               -- 实例配置 JSON(预留)
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_proj(tenant_id, project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目已安装组件';

-- 组件库种子（对齐项目工作区 Tab；自用租户 tenant_id=1）
INSERT INTO pm_component
 (id, tenant_id, code, name, icon, multi_instance, builtin, sort, is_deleted) VALUES
 (1,  1, 'overview',    '概览',   'Odometer',    0, 1, 10, 0),
 (2,  1, 'approval',    '立项',   'Stamp',       0, 1, 20, 0),
 (3,  1, 'info',        '信息',   'InfoFilled',  0, 1, 30, 0),
 (4,  1, 'task',        '任务',   'Tickets',     1, 1, 40, 0),
 (5,  1, 'goal',        '目标',   'Aim',         0, 1, 50, 0),
 (6,  1, 'stakeholder', '干系人', 'User',        0, 1, 60, 0),
 (7,  1, 'verify',      '验收',   'CircleCheck', 0, 1, 70, 0),
 (8,  1, 'gantt',       '甘特图', 'TrendCharts', 1, 1, 80, 0),
 (9,  1, 'cost',        '费用',   'Money',       0, 1, 90, 0),
 (10, 1, 'doc',         '文件',   'Folder',      0, 1, 100, 0),
 (11, 1, 'activity',    '活动',   'Clock',       0, 1, 110, 0);
