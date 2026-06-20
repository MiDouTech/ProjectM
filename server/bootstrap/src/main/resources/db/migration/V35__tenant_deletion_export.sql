-- V35 注销合规 + 数据导出（P2.2b）。
-- 租户增计划清除时间（注销宽限期满后清除）。
ALTER TABLE sys_tenant ADD COLUMN purge_scheduled_at DATETIME NULL COMMENT '注销清除计划时间' AFTER expire_at;

-- 租户数据导出任务（平台域全局表，已登记忽略名单）。异步：pending→处理→done，file_key 存对象存储键。
CREATE TABLE sys_tenant_export (
  id           BIGINT      NOT NULL PRIMARY KEY,
  tenant_id    BIGINT      NOT NULL,
  status       VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/done/failed',
  file_key     VARCHAR(512) COMMENT '对象存储 key(下载走预签名 URL)',
  error        VARCHAR(512),
  requested_by BIGINT,
  create_by    BIGINT,
  create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP,
  update_by    BIGINT,
  update_time  DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted   TINYINT     NOT NULL DEFAULT 0,
  KEY idx_export_tenant(tenant_id), KEY idx_export_status(status)
) COMMENT='租户数据导出任务(平台域)';
