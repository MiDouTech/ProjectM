-- V66 项目集成员：项目集由创建人 + 成员构成（对齐 Worktile）。创建人(owner)默认即首个成员。
-- 成员仅做归属/可见登记；项目集内项目的数据可见性仍受数据范围(data scope)约束。

CREATE TABLE pm_portfolio_member (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  portfolio_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_portfolio(tenant_id, portfolio_id), KEY idx_user(tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目集成员';
