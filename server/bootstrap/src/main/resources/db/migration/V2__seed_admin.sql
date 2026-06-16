-- V2 种子数据：内置管理员，便于本地首登与联调。
-- 账号 admin / 密码 admin123（BCrypt 哈希），租户固定 1（阶段一单租户）。
-- 生产环境请改密码或禁用该账号。

INSERT INTO sys_dept (id, tenant_id, name, parent_id, is_deleted)
VALUES (1, 1, '总部', 0, 0);

INSERT INTO sys_user (id, tenant_id, username, name, password, dept_id, job_level, status, is_deleted)
VALUES (1, 1, 'admin', '管理员',
        '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m',
        1, 'L3', 'active', 0);

INSERT INTO sys_role (id, tenant_id, name, code, is_deleted)
VALUES (1, 1, '超级管理员', 'admin', 0);

INSERT INTO sys_user_role (id, tenant_id, user_id, role_id, is_deleted)
VALUES (1, 1, 1, 1, 0);

-- 演示用权限码（与 module-org 控制器 @PreAuthorize 对齐）
INSERT INTO sys_role_perm (id, tenant_id, role_id, perm_code, is_deleted) VALUES
  (1, 1, 1, 'org:user:query',  0),
  (2, 1, 1, 'org:user:create', 0),
  (3, 1, 1, 'org:role:create', 0),
  (4, 1, 1, 'org:dept:create', 0);

-- 管理员对 user 资源数据范围 = 全部
INSERT INTO sys_role_data_scope (id, tenant_id, role_id, resource, scope, is_deleted)
VALUES (1, 1, 1, 'user', 'all', 0);
