-- V71 为内置超级管理员补充「配置管理」权限码 org:config:manage：
-- 治理导航编排(pm_module_nav)/页面表单配置(pm_page_config)/项目模板等租户级配置写操作，
-- 与各 @PreAuthorize('org:config:manage') 对齐。新租户由 OrgTenantProvisioner 同步播种此码。

-- 固定 id=1002：远离雪花区间，且避开 V2(1-4)/V34(5)/V64(1001) 既占用 id（原写死 5 与 V34 的
-- org:apikey:manage 撞 PK，致空库迁移在 V71 失败）。幂等：已授则不重复插。
INSERT INTO sys_role_perm (id, tenant_id, role_id, perm_code, is_deleted)
SELECT 1002, 1, 1, 'org:config:manage', 0
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_perm WHERE tenant_id = 1 AND role_id = 1 AND perm_code = 'org:config:manage'
);
