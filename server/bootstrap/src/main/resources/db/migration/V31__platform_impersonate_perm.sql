-- V31 平台域：为超管角色补充「模拟登录」权限码（P1 新增，不改已发布的 V28 种子）。
INSERT INTO sys_platform_role_perm (id, role_id, perm_code, is_deleted)
VALUES (10, 1, 'platform:tenant:impersonate', 0);
