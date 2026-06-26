-- V54 项目角色自定义化：将 pm_project_member.project_role 由自由文本/硬编码
-- 升级为租户可配置的「项目角色」字典（保留内置三档，向后兼容既有取值）。
-- 内置 code 即沿用现有中文取值（管理员/普通成员/只读成员），故旧成员行与前端无需迁移。
-- 公共字段对齐 pm_project_type；tenant_id 由多租户拦截器注入（种子按自用租户 tenant_id=1）。

CREATE TABLE pm_project_role (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  code VARCHAR(32),                          -- 角色编码（成员表 project_role 引用）
  name VARCHAR(32),                          -- 显示名
  builtin TINYINT DEFAULT 0,                 -- 1=内置(不可删)
  sort INT DEFAULT 0,
  status VARCHAR(16) DEFAULT 'active',       -- active/disabled
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_tenant(tenant_id), KEY idx_code(tenant_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目角色（租户自配）';

INSERT INTO pm_project_role
 (id, tenant_id, code, name, builtin, sort, status, is_deleted) VALUES
 (1, 1, '管理员',   '管理员',   1, 10, 'active', 0),
 (2, 1, '普通成员', '普通成员', 1, 20, 'active', 0),
 (3, 1, '只读成员', '只读成员', 1, 30, 'active', 0);
