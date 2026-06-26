-- V49 报表设置（业务域租户偏好）：财年起始月可配置（npss-rule §5 PMO 财年口径）。
--   pm_report_setting  每租户一行；fiscal_year_start_month=1 即自然年(默认)，=4 即 4 月起财年。
--   注：这是租户自身业务偏好（区别于平台域 sys_tenant 的运营属性），带 tenant_id 走多租户隔离。
CREATE TABLE pm_report_setting (
  id                      BIGINT  NOT NULL PRIMARY KEY,
  tenant_id               BIGINT  NOT NULL,
  fiscal_year_start_month TINYINT NOT NULL DEFAULT 1 COMMENT '财年起始月 1-12，默认1=自然年',
  create_by               BIGINT,
  create_time             DATETIME,
  update_by               BIGINT,
  update_time             DATETIME,
  is_deleted              TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant (tenant_id)
) COMMENT='报表设置（租户级，财年起始月等）';
