-- V28 平台域种子数据。
--   1) 平台超管账号 superadmin / superadmin123（BCrypt，与租户侧 admin 同算法；生产请改）。
--   2) 内置平台角色 super_admin（拥有全部平台权限码）。
--   3) 把现有「自用租户」登记为 sys_tenant id=1（与业务侧固定 tenant_id=1 对齐），不限期。
--   4) 内置套餐 免费版/标准版/旗舰版 + 配额；自用租户订阅旗舰版。

-- 平台超管账号（口令哈希对应 superadmin123）
INSERT INTO sys_platform_admin (id, username, password, name, status, is_deleted)
VALUES (1, 'superadmin', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m',
        '平台超级管理员', 'active', 0);

-- 平台角色：super_admin
INSERT INTO sys_platform_role (id, name, code, remark, is_deleted)
VALUES (1, '超级管理员', 'super_admin', '拥有平台全部权限', 0);

INSERT INTO sys_platform_admin_role (id, admin_id, role_id, is_deleted) VALUES (1, 1, 1, 0);

-- super_admin 全量权限码（与 PlatformPerms 对齐）
INSERT INTO sys_platform_role_perm (id, role_id, perm_code, is_deleted) VALUES
  (1, 1, 'platform:dashboard:view',     0),
  (2, 1, 'platform:tenant:query',       0),
  (3, 1, 'platform:tenant:manage',      0),
  (4, 1, 'platform:plan:query',         0),
  (5, 1, 'platform:plan:manage',        0),
  (6, 1, 'platform:subscription:manage',0),
  (7, 1, 'platform:admin:query',        0),
  (8, 1, 'platform:admin:manage',       0),
  (9, 1, 'platform:audit:query',        0);

-- 自用租户登记为 id=1（对齐业务侧固定 tenant_id=1），正式状态、不限期
INSERT INTO sys_tenant (id, code, name, status, industry, contact_name, source, activated_at, expire_at, is_deleted)
VALUES (1, 'mido', '米多（自用）', 'active', '互联网', '管理员', 'manual', NOW(), NULL, 0);

-- 内置套餐
INSERT INTO sys_plan (id, code, name, price, billing_cycle, status, sort, remark, is_deleted) VALUES
  (1, 'free',     '免费版', 0.00,      'yearly', 'active', 1, '入门体验',     0),
  (2, 'standard', '标准版', 9800.00,   'yearly', 'active', 2, '中小团队',     0),
  (3, 'flagship', '旗舰版', 39800.00,  'yearly', 'active', 3, '不限量+全功能', 0);

-- 套餐配额项（-1=不限）
INSERT INTO sys_plan_quota (id, plan_id, resource, limit_value, is_deleted) VALUES
  (1,  1, 'user',        10,    0),
  (2,  1, 'project',     5,     0),
  (3,  1, 'storage_mb',  1024,  0),
  (4,  1, 'task',        1000,  0),
  (5,  2, 'user',        50,    0),
  (6,  2, 'project',     50,    0),
  (7,  2, 'storage_mb',  10240, 0),
  (8,  2, 'task',        50000, 0),
  (9,  3, 'user',        -1,    0),
  (10, 3, 'project',     -1,    0),
  (11, 3, 'storage_mb',  -1,    0),
  (12, 3, 'task',        -1,    0);

-- 自用租户订阅旗舰版（不限期）
INSERT INTO sys_tenant_subscription (id, tenant_id, plan_id, start_at, expire_at, status, remark, is_deleted)
VALUES (1, 1, 3, NOW(), NULL, 'active', '自用租户内置订阅', 0);
