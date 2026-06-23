-- V43 简报评审（briefing.* P1）：提交后由作者部门负责人评审批注。
--   pm_briefing_recipient  评审人/抄送：提交时按作者部门负责人(sys_dept.leader_id)落 reviewer。
--   pm_briefing_review     评审批注：reviewer 对简报的评论/通过记录。

CREATE TABLE pm_briefing_recipient (
  id          BIGINT      NOT NULL PRIMARY KEY,
  tenant_id   BIGINT      NOT NULL,
  briefing_id BIGINT      NOT NULL COMMENT '简报(pm_briefing.id)',
  user_id     BIGINT      NOT NULL COMMENT '接收人(sys_user.id)',
  type        VARCHAR(16) NOT NULL DEFAULT 'reviewer' COMMENT 'reviewer 评审人 / cc 抄送',
  create_by   BIGINT,
  create_time DATETIME,
  update_by   BIGINT,
  update_time DATETIME,
  is_deleted  TINYINT     NOT NULL DEFAULT 0,
  KEY idx_user_type (user_id, type),
  KEY idx_briefing (briefing_id)
) COMMENT='简报评审人/抄送';

CREATE TABLE pm_briefing_review (
  id          BIGINT      NOT NULL PRIMARY KEY,
  tenant_id   BIGINT      NOT NULL,
  briefing_id BIGINT      NOT NULL COMMENT '简报(pm_briefing.id)',
  reviewer_id BIGINT      NOT NULL COMMENT '评审人(sys_user.id)',
  action      VARCHAR(16) NOT NULL DEFAULT 'comment' COMMENT 'comment 批注 / approve 已阅',
  comment     TEXT        COMMENT '批注内容',
  reviewed_at DATETIME    COMMENT '评审时间',
  create_by   BIGINT,
  create_time DATETIME,
  update_by   BIGINT,
  update_time DATETIME,
  is_deleted  TINYINT     NOT NULL DEFAULT 0,
  KEY idx_briefing (briefing_id)
) COMMENT='简报评审批注';
