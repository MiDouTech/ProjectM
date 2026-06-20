-- V32 平台域 P2.1：线下收入台账 / 公告 / 套餐功能开关。均为平台域全局表（已登记忽略名单）。
CREATE TABLE sys_revenue_record (
  id            BIGINT       NOT NULL PRIMARY KEY,
  tenant_id     BIGINT       NOT NULL COMMENT '指向 sys_tenant.id 的普通引用列',
  type          VARCHAR(16)  NOT NULL DEFAULT 'payment' COMMENT 'payment 收款 / refund 退款',
  amount        DECIMAL(14,2) NOT NULL COMMENT '金额',
  contract_no   VARCHAR(64)  COMMENT '合同号',
  occurred_date DATE         COMMENT '发生日期',
  remark        VARCHAR(512),
  create_by     BIGINT,
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by     BIGINT,
  update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  KEY idx_rev_tenant(tenant_id), KEY idx_rev_date(occurred_date)
) COMMENT='线下收入台账(平台域)';

CREATE TABLE sys_announcement (
  id          BIGINT       NOT NULL PRIMARY KEY,
  title       VARCHAR(256) NOT NULL,
  content     TEXT         NOT NULL,
  level       VARCHAR(16)  DEFAULT 'info' COMMENT 'info/warning',
  status      VARCHAR(16)  NOT NULL DEFAULT 'draft' COMMENT 'draft/published',
  publish_at  DATETIME,
  expire_at   DATETIME,
  create_by   BIGINT,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT      NOT NULL DEFAULT 0,
  KEY idx_ann_status(status)
) COMMENT='平台公告(平台域)';

CREATE TABLE sys_plan_feature (
  id           BIGINT      NOT NULL PRIMARY KEY,
  plan_id      BIGINT      NOT NULL,
  feature_code VARCHAR(32) NOT NULL COMMENT '取自 FeatureCodes',
  enabled      TINYINT     NOT NULL DEFAULT 1,
  create_by    BIGINT,
  create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP,
  update_by    BIGINT,
  update_time  DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted   TINYINT     NOT NULL DEFAULT 0,
  KEY idx_pf_plan(plan_id)
) COMMENT='套餐功能开关(平台域)';
