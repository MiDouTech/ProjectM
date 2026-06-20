-- V27 平台域（SaaS 运营总后台）建表。
--   平台域是跨租户全局域，所有表【不带 tenant_id】，登记在 MidoTenantLineHandler 忽略名单，不参与多租户隔离。
--   这是对「所有业务表必带 tenant_id」的正式架构例外（见 docs/data-model.md「平台域」与 com.mido.pm.platform 包说明）。
--   公共字段：id(雪花) / create_by / create_time / update_by / update_time / is_deleted。审计表为追加写、无逻辑删除。

-- 租户注册表：本表 id 即业务侧各表的 tenant_id。
CREATE TABLE sys_tenant (
  id            BIGINT       NOT NULL PRIMARY KEY,
  code          VARCHAR(32)  NOT NULL COMMENT '租户编码(程序引用/子域名,全局唯一)',
  name          VARCHAR(128) NOT NULL COMMENT '租户名称',
  status        VARCHAR(16)  NOT NULL DEFAULT 'trial' COMMENT 'trial/active/suspended/expired/closed',
  industry      VARCHAR(64)  COMMENT '行业',
  contact_name  VARCHAR(64)  COMMENT '联系人',
  contact_phone VARCHAR(32)  COMMENT '联系电话',
  contact_email VARCHAR(128) COMMENT '联系邮箱',
  source        VARCHAR(16)  DEFAULT 'manual' COMMENT '来源:manual 运营手动',
  admin_user_id BIGINT       COMMENT '租户主管理员 sys_user.id(P1 自动初始化)',
  activated_at  DATETIME     COMMENT '首次激活时间',
  expire_at     DATETIME     COMMENT '到期时间(随订阅,空=不限期)',
  remark        VARCHAR(512) COMMENT '备注',
  create_by     BIGINT,
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by     BIGINT,
  update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tenant_code(code),
  KEY idx_tenant_status(status),
  KEY idx_tenant_expire(expire_at)
) COMMENT='租户注册表(平台域,无 tenant_id)';

-- 套餐
CREATE TABLE sys_plan (
  id            BIGINT       NOT NULL PRIMARY KEY,
  code          VARCHAR(32)  NOT NULL COMMENT '套餐编码(全局唯一)',
  name          VARCHAR(64)  NOT NULL,
  price         DECIMAL(14,2) COMMENT '线下参考价(不接支付网关)',
  billing_cycle VARCHAR(16)  DEFAULT 'yearly' COMMENT 'monthly/yearly/once',
  status        VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT 'active/disabled',
  sort          INT          DEFAULT 0,
  remark        VARCHAR(255),
  create_by     BIGINT,
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by     BIGINT,
  update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_plan_code(code)
) COMMENT='套餐(平台域)';

-- 套餐配额项
CREATE TABLE sys_plan_quota (
  id          BIGINT      NOT NULL PRIMARY KEY,
  plan_id     BIGINT      NOT NULL,
  resource    VARCHAR(32) NOT NULL COMMENT 'user/project/storage_mb/task...',
  limit_value BIGINT      NOT NULL DEFAULT -1 COMMENT '上限,-1=不限',
  create_by   BIGINT,
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT     NOT NULL DEFAULT 0,
  KEY idx_quota_plan(plan_id)
) COMMENT='套餐配额项(平台域)';

-- 租户订阅(每租户至多一条 active)
CREATE TABLE sys_tenant_subscription (
  id             BIGINT      NOT NULL PRIMARY KEY,
  tenant_id      BIGINT      NOT NULL COMMENT '指向 sys_tenant.id 的普通引用列(非隔离列)',
  plan_id        BIGINT      NOT NULL,
  start_at       DATETIME    COMMENT '生效时间',
  expire_at      DATETIME    COMMENT '到期时间(空=不限期)',
  status         VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active/expired/cancelled',
  quota_override TEXT        COMMENT '配额覆盖(JSON,按 resource 覆盖套餐默认)',
  remark         VARCHAR(255),
  create_by      BIGINT,
  create_time    DATETIME    DEFAULT CURRENT_TIMESTAMP,
  update_by      BIGINT,
  update_time    DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted     TINYINT     NOT NULL DEFAULT 0,
  KEY idx_sub_tenant(tenant_id, status),
  KEY idx_sub_plan(plan_id)
) COMMENT='租户订阅(平台域)';

-- 平台运营账号(独立账号体系,不属于任何租户)
CREATE TABLE sys_platform_admin (
  id            BIGINT       NOT NULL PRIMARY KEY,
  username      VARCHAR(64)  NOT NULL COMMENT '登录名(全局唯一)',
  password      VARCHAR(128) NOT NULL COMMENT 'BCrypt 哈希',
  name          VARCHAR(64)  NOT NULL,
  status        VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT 'active/disabled',
  last_login_at DATETIME,
  create_by     BIGINT,
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by     BIGINT,
  update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_padmin_username(username)
) COMMENT='平台运营账号(平台域)';

-- 平台角色
CREATE TABLE sys_platform_role (
  id          BIGINT      NOT NULL PRIMARY KEY,
  name        VARCHAR(64) NOT NULL,
  code        VARCHAR(64) NOT NULL COMMENT '角色编码(全局唯一)',
  remark      VARCHAR(255),
  create_by   BIGINT,
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT     NOT NULL DEFAULT 0,
  UNIQUE KEY uk_prole_code(code)
) COMMENT='平台角色(平台域)';

-- 平台账号-角色
CREATE TABLE sys_platform_admin_role (
  id          BIGINT   NOT NULL PRIMARY KEY,
  admin_id    BIGINT   NOT NULL,
  role_id     BIGINT   NOT NULL,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  NOT NULL DEFAULT 0,
  KEY idx_par_admin(admin_id)
) COMMENT='平台账号-角色(平台域)';

-- 平台角色-权限码
CREATE TABLE sys_platform_role_perm (
  id          BIGINT      NOT NULL PRIMARY KEY,
  role_id     BIGINT      NOT NULL,
  perm_code   VARCHAR(64) NOT NULL COMMENT '取自 PlatformPerms',
  create_by   BIGINT,
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT     NOT NULL DEFAULT 0,
  KEY idx_prp_role(role_id)
) COMMENT='平台角色-权限码(平台域)';

-- 平台运营审计(追加写,无逻辑删除)
CREATE TABLE sys_platform_audit_log (
  id          BIGINT       NOT NULL PRIMARY KEY,
  admin_id    BIGINT       COMMENT '操作的平台账号',
  action      VARCHAR(64)  NOT NULL COMMENT '动作码,取自 PlatformAuditActions',
  target      VARCHAR(32)  COMMENT 'tenant/plan/subscription/admin',
  target_id   BIGINT,
  detail      TEXT         COMMENT '明细 JSON',
  ip          VARCHAR(64),
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  KEY idx_paudit_target(target, target_id),
  KEY idx_paudit_admin(admin_id)
) COMMENT='平台运营审计(平台域,追加写)';
