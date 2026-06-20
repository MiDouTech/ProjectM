-- V30 平台域：租户用量快照表（P1 用量统计）。
--   平台域全局表（不参与多租户隔离，登记在 MidoTenantLineHandler 忽略名单）；
--   tenant_id 为指向 sys_tenant.id 的普通引用列。每个 (tenant, resource) 一行，定时任务 upsert 当前用量。
CREATE TABLE sys_tenant_quota_usage (
  id            BIGINT      NOT NULL PRIMARY KEY,
  tenant_id     BIGINT      NOT NULL COMMENT '指向 sys_tenant.id 的普通引用列',
  resource      VARCHAR(32) NOT NULL COMMENT 'user/project/task/storage_mb',
  used_value    BIGINT      NOT NULL DEFAULT 0 COMMENT '当前用量',
  snapshot_time DATETIME    COMMENT '最近一次快照时间',
  create_by     BIGINT,
  create_time   DATETIME    DEFAULT CURRENT_TIMESTAMP,
  update_by     BIGINT,
  update_time   DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT     NOT NULL DEFAULT 0,
  UNIQUE KEY uk_usage_tenant_res(tenant_id, resource)
) COMMENT='租户用量快照(平台域)';
