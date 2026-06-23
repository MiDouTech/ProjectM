-- V44 简报跟进问题（briefing.* P2）：从简报/评审中提出待跟进问题，指派负责人、跟踪状态。
--   pm_briefing_issue：raised_by 提出人、owner_id 负责人、status open/following/closed、due_date 截止。

CREATE TABLE pm_briefing_issue (
  id          BIGINT      NOT NULL PRIMARY KEY,
  tenant_id   BIGINT      NOT NULL,
  briefing_id BIGINT      NOT NULL COMMENT '来源简报(pm_briefing.id)',
  raised_by   BIGINT      NOT NULL COMMENT '提出人(sys_user.id)',
  owner_id    BIGINT      COMMENT '负责人(sys_user.id)',
  content     VARCHAR(512) NOT NULL COMMENT '问题内容',
  status      VARCHAR(16) NOT NULL DEFAULT 'open' COMMENT 'open 待处理 / following 跟进中 / closed 已关闭',
  due_date    DATE        COMMENT '截止日期',
  create_by   BIGINT,
  create_time DATETIME,
  update_by   BIGINT,
  update_time DATETIME,
  is_deleted  TINYINT     NOT NULL DEFAULT 0,
  KEY idx_owner (owner_id),
  KEY idx_raised (raised_by),
  KEY idx_briefing (briefing_id)
) COMMENT='简报跟进问题';
