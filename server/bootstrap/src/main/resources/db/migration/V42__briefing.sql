-- V42 简报域（briefing.*）P0：人工日/周/月报模板 + 填报。区别于 PMO 度量 module-report。
--   pm_briefing_template  模板：type=daily/weekly/monthly；schema=字段定义 JSON 数组 [{key,label,type}]。
--                         内置日/周/月报由 BriefingTemplateService 按租户惰性生成。
--   pm_briefing           简报实例：period_key 标识周期(2026-06-23 / 2026-W26 / 2026-06)；
--                         content=按 schema 填写内容 JSON；status=draft/submitted。

CREATE TABLE pm_briefing_template (
  id          BIGINT      NOT NULL PRIMARY KEY,
  tenant_id   BIGINT      NOT NULL,
  name        VARCHAR(64) NOT NULL COMMENT '模板名称',
  type        VARCHAR(16) NOT NULL COMMENT 'daily 日报 / weekly 周报 / monthly 月报',
  schema      JSON        COMMENT '字段定义数组 [{key,label,type}]',
  scope       VARCHAR(16) NOT NULL DEFAULT 'tenant' COMMENT '适用范围: tenant 全租户(P0) / dept / custom',
  is_builtin  TINYINT     NOT NULL DEFAULT 0 COMMENT '是否内置模板',
  status      VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active 启用 / disabled 停用',
  create_by   BIGINT,
  create_time DATETIME,
  update_by   BIGINT,
  update_time DATETIME,
  is_deleted  TINYINT     NOT NULL DEFAULT 0,
  KEY idx_tenant_type (tenant_id, type)
) COMMENT='简报模板';

CREATE TABLE pm_briefing (
  id           BIGINT      NOT NULL PRIMARY KEY,
  tenant_id    BIGINT      NOT NULL,
  template_id  BIGINT      NOT NULL COMMENT '模板(pm_briefing_template.id)',
  type         VARCHAR(16) NOT NULL COMMENT 'daily/weekly/monthly',
  author_id    BIGINT      NOT NULL COMMENT '提交人(sys_user.id)',
  dept_id      BIGINT      COMMENT '提交人部门, 数据范围用',
  period_key   VARCHAR(32) NOT NULL COMMENT '周期标识: 2026-06-23 / 2026-W26 / 2026-06',
  period_start DATE        COMMENT '周期起',
  period_end   DATE        COMMENT '周期止',
  content      JSON        COMMENT '按 schema 填写内容 {key:value}',
  status       VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT 'draft 草稿 / submitted 已提交',
  submitted_at DATETIME    COMMENT '提交时间',
  create_by    BIGINT,
  create_time  DATETIME,
  update_by    BIGINT,
  update_time  DATETIME,
  is_deleted   TINYINT     NOT NULL DEFAULT 0,
  KEY idx_author_type (author_id, type),
  KEY idx_period (tenant_id, template_id, author_id, period_key)
) COMMENT='简报实例';
