-- V71 为内置超级管理员补充「配置管理」权限码 org:config:manage：
-- 治理导航编排(pm_module_nav)/页面表单配置(pm_page_config)/项目模板等租户级配置写操作，
-- 与各 @PreAuthorize('org:config:manage') 对齐。新租户由 OrgTenantProvisioner 同步播种此码。

INSERT INTO sys_role_perm (id, tenant_id, role_id, perm_code, is_deleted)
VALUES (5, 1, 1, 'org:config:manage', 0);
