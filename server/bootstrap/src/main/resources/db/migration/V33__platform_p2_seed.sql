-- V33 平台域 P2.1 种子：超管补权限码 + 内置套餐默认功能开关。
INSERT INTO sys_platform_role_perm (id, role_id, perm_code, is_deleted) VALUES
  (11, 1, 'platform:revenue:query',       0),
  (12, 1, 'platform:revenue:manage',      0),
  (13, 1, 'platform:announcement:manage', 0),
  (14, 1, 'platform:feature:manage',      0);

-- 内置套餐默认功能开关：旗舰版(3)全开；标准版(2)除 openapi 外全开；免费版(1)仅基础。
-- 自用租户订阅旗舰版，故默认全功能可用。
INSERT INTO sys_plan_feature (id, plan_id, feature_code, enabled, is_deleted) VALUES
  -- flagship (plan 3) 全开
  (10301, 3, 'gantt',   1, 0), (10302, 3, 'okr',    1, 0), (10303, 3, 'npss',   1, 0),
  (10304, 3, 'doc',     1, 0), (10305, 3, 'cost',   1, 0), (10306, 3, 'report', 1, 0),
  (10307, 3, 'change',  1, 0), (10308, 3, 'openapi',1, 0),
  -- standard (plan 2) 除 openapi 外全开
  (10201, 2, 'gantt',   1, 0), (10202, 2, 'okr',    1, 0), (10203, 2, 'npss',   1, 0),
  (10204, 2, 'doc',     1, 0), (10205, 2, 'cost',   1, 0), (10206, 2, 'report', 1, 0),
  (10207, 2, 'change',  1, 0), (10208, 2, 'openapi',0, 0),
  -- free (plan 1) 仅基础
  (10101, 1, 'gantt',   0, 0), (10102, 1, 'okr',    1, 0), (10103, 1, 'npss',   0, 0),
  (10104, 1, 'doc',     1, 0), (10105, 1, 'cost',   0, 0), (10106, 1, 'report', 1, 0),
  (10107, 1, 'change',  0, 0), (10108, 1, 'openapi',0, 0);
