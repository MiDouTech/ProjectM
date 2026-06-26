-- V64 给自用租户(tenant 1)超级管理员补授 org:audit:query 权限码。
-- 背景：租户级操作日志查询端点 @PreAuthorize 接受 org:audit:query（或回退建/管角色权限）。
--   新租户开通时 OrgTenantProvisioner 已直接授予该权限码；但 tenant 1 是 V2 迁移种子，
--   彼时仅授 4 个权限码、靠回退访问审计。此处补齐，使其与新租户一致、可单独授审计角色。
-- 幂等：仅当尚未授予时插入；固定 id 远离雪花区间且避开 V2 既有 1-4。
INSERT INTO sys_role_perm (id, tenant_id, role_id, perm_code, is_deleted)
SELECT 1001, 1, 1, 'org:audit:query', 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_perm WHERE tenant_id = 1 AND role_id = 1 AND perm_code = 'org:audit:query'
);
