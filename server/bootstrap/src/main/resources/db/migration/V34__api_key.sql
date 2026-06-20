-- V34 开放平台 API Key（P2.2a）。租户业务表（带 tenant_id，租户隔离）。
--   key 绑定某用户：携带 key 调用 OpenAPI 等同该用户身份。仅存 SHA-256 与前缀，明文仅创建时返回一次。
CREATE TABLE sys_api_key (
  id           BIGINT       NOT NULL PRIMARY KEY,
  tenant_id    BIGINT       NOT NULL,
  user_id      BIGINT       NOT NULL COMMENT '绑定用户(继承其权限/数据范围)',
  name         VARCHAR(64)  NOT NULL,
  key_hash     CHAR(64)     NOT NULL COMMENT 'key 的 SHA-256 十六进制(鉴权查找)',
  key_prefix   VARCHAR(16)  COMMENT '前缀(展示, 如 mk_xxxxxxxx)',
  status       VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT 'active/disabled',
  last_used_at DATETIME,
  expire_at    DATETIME     COMMENT '到期时间, 空=长期有效',
  create_by    BIGINT,
  create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by    BIGINT,
  update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted   TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_apikey_hash(key_hash),
  KEY idx_apikey_tenant(tenant_id)
) COMMENT='开放平台 API Key(租户业务表)';

-- 租户内置超管角色补「API Key 管理」权限码（V2 内置 role_id=1 / tenant_id=1）
INSERT INTO sys_role_perm (id, tenant_id, role_id, perm_code, is_deleted)
VALUES (5, 1, 1, 'org:apikey:manage', 0);
