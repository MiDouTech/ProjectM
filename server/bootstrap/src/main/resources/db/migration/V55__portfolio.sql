-- V55 项目集（portfolio）：管理层通过项目集查看跨部门「全公司项目总览」。
-- 项目集是项目的逻辑分组容器；总览的项目可见范围仍受数据范围(data scope)约束——
-- 管理层(ALL)看全量，部门成员看其权限内项目，天然实现全局视图 vs 局部视图隔离。
-- 公共字段对齐其余业务表；tenant_id 由多租户拦截器注入。

CREATE TABLE pm_portfolio (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  name VARCHAR(128),
  description VARCHAR(512),
  owner_id BIGINT,                           -- 项目集负责人
  status VARCHAR(16) DEFAULT 'active',       -- active/archived
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_tenant(tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目集';

CREATE TABLE pm_portfolio_project (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  portfolio_id BIGINT,
  project_id BIGINT,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_portfolio(tenant_id, portfolio_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目集-项目关联';
