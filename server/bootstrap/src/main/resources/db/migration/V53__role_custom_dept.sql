-- V53 数据范围增强：持久化角色的「自定义部门集」（CUSTOM 数据范围所需）。
-- 此前 CurrentUser.customDeptIds 仅有内存字段、无持久化（注释标注待补）；本表补齐。
-- 角色任一资源数据范围取 custom 时，该角色的自定义部门集（本表）即 WHERE dept_id IN (...) 依据。
-- 公共字段对齐 sys_role_data_scope；tenant_id 由多租户拦截器注入。

CREATE TABLE sys_role_custom_dept (
  id BIGINT PRIMARY KEY, tenant_id BIGINT, role_id BIGINT, dept_id BIGINT,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_role_custom_dept (tenant_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色自定义部门集（custom 数据范围）';
