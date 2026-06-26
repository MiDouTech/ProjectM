-- V48 NPSS 评价主体（npss-rule §3/§4）：把评分单位从"干系人个人加权"升级为"评价主体加权 + 组内平均"。
--   pm_npss_subject_template  租户级评价主体模板：name 主体名 / weight 权重 / beneficiary 是否受益方(≥50% 校验) /
--                             enabled 启用。合计=100% 校验在 Service 层。
--   pm_npss_subject           项目级评价主体实例：从租户模板物化，项目可覆盖增删/改权重。
--   pm_npss_subject_member    主体成员：成员即干系人(pm_stakeholder.id)，一个主体可多人；算分时组内平均。
--   pm_npss_score 追加 subject_id：评分归属主体(快照)，汇总时按主体分组先平均再加权。

CREATE TABLE pm_npss_subject_template (
  id          BIGINT       NOT NULL PRIMARY KEY,
  tenant_id   BIGINT       NOT NULL,
  name        VARCHAR(64)  NOT NULL COMMENT '评价主体名称',
  weight      DECIMAL(5,2) NOT NULL COMMENT '权重占比(%)，启用主体合计=100',
  beneficiary TINYINT      NOT NULL DEFAULT 0 COMMENT '是否受益方 1是/0否（受益方合计须≥50%）',
  sort        INT          NOT NULL DEFAULT 0 COMMENT '展示排序',
  enabled     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用 1是/0否',
  create_by   BIGINT,
  create_time DATETIME,
  update_by   BIGINT,
  update_time DATETIME,
  is_deleted  TINYINT      NOT NULL DEFAULT 0,
  KEY idx_tenant (tenant_id)
) COMMENT='NPSS 评价主体模板（租户级）';

CREATE TABLE pm_npss_subject (
  id          BIGINT       NOT NULL PRIMARY KEY,
  tenant_id   BIGINT       NOT NULL,
  project_id  BIGINT       NOT NULL COMMENT '所属项目(pm_project.id)',
  template_id BIGINT       COMMENT '来源模板(pm_npss_subject_template.id)，覆盖新增可为空',
  name        VARCHAR(64)  NOT NULL COMMENT '评价主体名称',
  weight      DECIMAL(5,2) NOT NULL COMMENT '权重占比(%)，启用主体合计=100',
  beneficiary TINYINT      NOT NULL DEFAULT 0 COMMENT '是否受益方 1是/0否',
  sort        INT          NOT NULL DEFAULT 0 COMMENT '展示排序',
  create_by   BIGINT,
  create_time DATETIME,
  update_by   BIGINT,
  update_time DATETIME,
  is_deleted  TINYINT      NOT NULL DEFAULT 0,
  KEY idx_proj (project_id)
) COMMENT='NPSS 评价主体（项目级实例）';

CREATE TABLE pm_npss_subject_member (
  id             BIGINT  NOT NULL PRIMARY KEY,
  tenant_id      BIGINT  NOT NULL,
  subject_id     BIGINT  NOT NULL COMMENT '评价主体(pm_npss_subject.id)',
  stakeholder_id BIGINT  NOT NULL COMMENT '成员=干系人(pm_stakeholder.id)',
  create_by      BIGINT,
  create_time    DATETIME,
  update_by      BIGINT,
  update_time    DATETIME,
  is_deleted     TINYINT NOT NULL DEFAULT 0,
  KEY idx_subject (subject_id)
) COMMENT='NPSS 评价主体成员（成员即干系人）';

ALTER TABLE pm_npss_score
  ADD COLUMN subject_id BIGINT COMMENT '评价主体(pm_npss_subject.id)快照，汇总按主体分组先平均再加权' AFTER stakeholder_id;
