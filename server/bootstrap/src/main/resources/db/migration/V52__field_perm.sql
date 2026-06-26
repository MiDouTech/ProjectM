-- V52 字段级权限（三级权限的「任务属性」层）：按角色对单个字段配置「仅查看(view)/可编辑(edit)」。
-- 默认（未配置）= 可编辑，遵循 opt-in 收紧规则；多角色合并取最宽（任一可编辑即可编辑）。
-- 公共字段对齐 sys_role_data_scope；tenant_id 由多租户拦截器注入。

CREATE TABLE sys_field_perm (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, role_id BIGINT,
  resource VARCHAR(32),                     -- 资源：task/project
  field    VARCHAR(64),                     -- 字段键（如 priority/status/assignee）
  access   VARCHAR(8),                       -- view 仅查看 / edit 可编辑
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_field_perm_role (tenant_id, role_id, resource)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色字段级权限';
