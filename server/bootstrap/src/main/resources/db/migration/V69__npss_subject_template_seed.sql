-- V69 自用租户(tenant_id=1) NPSS 默认评价主体模板种子（core-business-flow 体检断点8：此前无默认，NPSS 不开箱即用）。
-- 新租户由 NpssTemplateTenantProvisioner 播种；本迁移补存量自用租户。
-- 幂等：仅当 tenant 1 尚无主体模板时插入，避免覆盖管理员已有配置。
-- 默认：发起人30/业务方30(受益方,合计60≥50%)/团队10/财务10/其他20，启用合计=100%，满足 SubjectWeightValidator。

INSERT INTO pm_npss_subject_template (id, tenant_id, name, weight, beneficiary, sort, enabled, is_deleted)
SELECT * FROM (
  SELECT 1 AS id, 1 AS tenant_id, '发起人' AS name, 30 AS weight, 1 AS beneficiary, 0 AS sort, 1 AS enabled, 0 AS is_deleted
  UNION ALL SELECT 2, 1, '业务方', 30, 1, 1, 1, 0
  UNION ALL SELECT 3, 1, '团队',   10, 0, 2, 1, 0
  UNION ALL SELECT 4, 1, '财务',   10, 0, 3, 1, 0
  UNION ALL SELECT 5, 1, '其他',   20, 0, 4, 1, 0
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM pm_npss_subject_template WHERE tenant_id = 1 AND is_deleted = 0
);
