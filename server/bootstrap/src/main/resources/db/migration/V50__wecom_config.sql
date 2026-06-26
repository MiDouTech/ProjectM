-- V50 企业微信集成配置（pm_wecom_config）：租户自助配置企微凭证与开关的可视化入口。
-- 业务表（带 tenant_id，多租户隔离）。secret 列存 AES 密文（SecretCipher 加密），明文不入库、不出接口。
-- 单租户一行：由 WecomConfigService 以 selectOne + insert/update 维护，不加唯一约束以兼容逻辑删除。
CREATE TABLE pm_wecom_config (
  id               BIGINT       NOT NULL PRIMARY KEY,
  tenant_id        BIGINT       NOT NULL,
  corp_id          VARCHAR(64)  COMMENT '企业 ID（CorpID），三类能力共用',
  -- 通讯录同步
  contacts_enabled TINYINT      NOT NULL DEFAULT 0 COMMENT '通讯录同步开关',
  contacts_secret  VARCHAR(512) COMMENT '通讯录 Secret（AES 密文）',
  -- SSO 登录
  sso_enabled      TINYINT      NOT NULL DEFAULT 0 COMMENT '扫码/OAuth 登录开关',
  sso_agent_id     VARCHAR(32)  COMMENT 'SSO 应用 AgentId',
  sso_secret       VARCHAR(512) COMMENT 'SSO Secret（AES 密文）',
  -- 消息推送
  msg_enabled      TINYINT      NOT NULL DEFAULT 0 COMMENT '消息推送开关',
  msg_agent_id     VARCHAR(32)  COMMENT '消息应用 AgentId',
  msg_secret       VARCHAR(512) COMMENT '消息 Secret（AES 密文）',
  -- 同步状态（展示用）
  last_sync_at     DATETIME     COMMENT '最近一次通讯录同步时间',
  last_sync_result VARCHAR(256) COMMENT '最近一次同步结果摘要',
  create_by        BIGINT,
  create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by        BIGINT,
  update_time      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted       TINYINT      NOT NULL DEFAULT 0,
  KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业微信集成配置';
