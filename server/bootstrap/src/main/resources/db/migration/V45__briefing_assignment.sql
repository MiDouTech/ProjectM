-- V45 简报指派（briefing.* P2）：模板指派给用户/部门，决定「我应交」的模板。
--   pm_briefing_assignment：target_type=user/dept，target_id 指向 sys_user/sys_dept。

CREATE TABLE pm_briefing_assignment (
  id          BIGINT      NOT NULL PRIMARY KEY,
  tenant_id   BIGINT      NOT NULL,
  template_id BIGINT      NOT NULL COMMENT '模板(pm_briefing_template.id)',
  target_type VARCHAR(16) NOT NULL DEFAULT 'user' COMMENT 'user 用户 / dept 部门',
  target_id   BIGINT      NOT NULL COMMENT '目标 id(sys_user/sys_dept)',
  create_by   BIGINT,
  create_time DATETIME,
  update_by   BIGINT,
  update_time DATETIME,
  is_deleted  TINYINT     NOT NULL DEFAULT 0,
  KEY idx_template (template_id),
  KEY idx_target (target_type, target_id)
) COMMENT='简报模板指派';
