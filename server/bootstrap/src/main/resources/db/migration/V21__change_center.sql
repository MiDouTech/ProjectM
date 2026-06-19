-- V21 变更中心（通用变更域）：受控变更——改业务基线须走变更单，复用审批引擎，通过即回写并归档。
--   pm_change_request 变更单台账（变更中心）；pm_change_policy 按变更类型可配审批。
--   biz_type=被改实体域(goal/未来 project/cost)；change_type=细分类型；before/after 为快照(JSON 文本)。

CREATE TABLE pm_change_request (
  id            BIGINT       NOT NULL PRIMARY KEY,
  tenant_id     BIGINT       NOT NULL,
  biz_type      VARCHAR(32)  NOT NULL COMMENT '被改实体域: goal(未来 project/cost)',
  biz_id        BIGINT       NOT NULL COMMENT '被改实体 id(如 goalId)',
  change_type   VARCHAR(32)  NOT NULL COMMENT '细分类型: goal_target/goal_scope/goal_owner/goal_period/goal_close',
  title         VARCHAR(256) COMMENT '变更摘要',
  reason        VARCHAR(512) NOT NULL COMMENT '变更事由(留痕核心)',
  impact        VARCHAR(512) COMMENT '影响分析',
  before_snapshot TEXT       COMMENT '变更前快照(JSON)=基线记录',
  after_payload   TEXT       COMMENT '拟定新值(JSON,仅改动字段)',
  status        VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'draft/pending/approved/applied/rejected/withdrawn',
  approval_instance_id BIGINT COMMENT '关联审批实例(免审为空)',
  applied_at    DATETIME     COMMENT '生效时间',
  create_by     BIGINT,
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by     BIGINT,
  update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  -- 在途键：仅当 pending 且未删除时取 biz_id，否则 NULL。配合唯一索引，由 DB 保证「每对象至多一张在途变更单」
  -- （MySQL 唯一索引允许多个 NULL，故终态/已删行不占位）；兜住 hasPending 预检的 TOCTOU 竞态。
  pending_key   BIGINT GENERATED ALWAYS AS (IF(status = 'pending' AND is_deleted = 0, biz_id, NULL)) STORED,
  KEY idx_biz(biz_type, biz_id),
  KEY idx_tenant_status(tenant_id, status),
  UNIQUE KEY uk_pending(tenant_id, biz_type, pending_key)
) COMMENT='变更单台账(变更中心)';

CREATE TABLE pm_change_policy (
  id            BIGINT       NOT NULL PRIMARY KEY,
  tenant_id     BIGINT       NOT NULL,
  change_type   VARCHAR(32)  NOT NULL COMMENT '变更类型',
  require_approval TINYINT   NOT NULL DEFAULT 1 COMMENT '1必审/0免审仅留痕',
  flow_id       BIGINT       COMMENT '必审时绑定的审批流',
  enabled       TINYINT      NOT NULL DEFAULT 1,
  create_by     BIGINT,
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by     BIGINT,
  update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  KEY idx_tenant_type(tenant_id, change_type)
) COMMENT='变更审批策略(按类型可配)';

-- 种子（默认租户 1）：重大字段必审、轻微调整免审。必审类型 flow_id 暂空，由租户在配置中心绑定审批流后方可提交。
-- goal_close 暂不纳入：pm_goal 无状态列，待目标状态机落地再补策略。
INSERT INTO pm_change_policy (id, tenant_id, change_type, require_approval, flow_id, enabled, create_time, update_time, is_deleted) VALUES
  (90001, 1, 'goal_target', 1, NULL, 1, NOW(), NOW(), 0),
  (90002, 1, 'goal_scope',  1, NULL, 1, NOW(), NOW(), 0),
  (90004, 1, 'goal_owner',  0, NULL, 1, NOW(), NOW(), 0),
  (90005, 1, 'goal_period', 0, NULL, 1, NOW(), NOW(), 0);
