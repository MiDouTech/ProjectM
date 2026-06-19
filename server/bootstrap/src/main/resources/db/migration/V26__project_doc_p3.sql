-- V26 文档模块 P3：文档级权限 ACL + 公开分享外链。
--   pm_doc_acl   节点授权：principal(user/role) × permission(read/write/admin)，就近继承。
--   pm_doc_share 公开分享：token 免登录只读访问，可设过期/停用。

CREATE TABLE pm_doc_acl (
  id             BIGINT      NOT NULL PRIMARY KEY,
  tenant_id      BIGINT      NOT NULL,
  doc_id         BIGINT      NOT NULL COMMENT '授权作用的节点(pm_doc.id)',
  principal_type VARCHAR(8)  NOT NULL COMMENT '主体类型: user / role',
  principal_id   BIGINT      NOT NULL COMMENT '用户 id 或 角色 id',
  permission     VARCHAR(8)  NOT NULL COMMENT '权限: read / write / admin',
  create_by      BIGINT,
  create_time    DATETIME,
  update_by      BIGINT,
  update_time    DATETIME,
  is_deleted     TINYINT     NOT NULL DEFAULT 0,
  KEY idx_doc (doc_id)
) COMMENT='文档级权限授权';

CREATE TABLE pm_doc_share (
  id          BIGINT       NOT NULL PRIMARY KEY,
  tenant_id   BIGINT       NOT NULL,
  doc_id      BIGINT       NOT NULL COMMENT '被分享文档(pm_doc.id)',
  token       VARCHAR(64)  NOT NULL COMMENT '分享令牌(全局唯一)',
  expire_time DATETIME     COMMENT '过期时间(空=永久)',
  enabled     TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
  create_by   BIGINT,
  create_time DATETIME,
  update_by   BIGINT,
  update_time DATETIME,
  is_deleted  TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_token (token),
  KEY idx_doc (doc_id)
) COMMENT='文档公开分享外链';
