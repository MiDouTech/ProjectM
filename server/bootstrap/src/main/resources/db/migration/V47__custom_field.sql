-- V47 自定义字段（EAV）：租户自配字段定义 + 字段值。
--   pm_field_def    字段定义：scope=task/project，类型(text/number/date/select/multi_select/checkbox/user)，
--                   select/multi_select 用 options(JSON) 承载选项；required 必填、sort_no 排序、enabled 启停。
--                   字段在(租户,scope)内以 field_key 唯一（停用/逻辑删后允许重建，唯一性由 Service 校验，不建 DB 唯一键）。
--   pm_field_value  字段值：EAV，entity_type=task/project + entity_id 指向业务实体，value 统一文本存储，
--                   多选/用户型存 JSON 字符串。每个(entity,field)至多一行有效值，upsert 由 Service 保证。
--   说明：取代 pm_task.custom_fields JSON 列（旧列弃用留存，不迁移不删除）；任务/项目详情经 /api/v1/field-values 读写。
--   字段值变更并入业务实体活动流（AuditLog），不新增 Outbox 事件名（遵循 docs/domain-events.md 事件清单）。

CREATE TABLE pm_field_def (
  id          BIGINT      NOT NULL PRIMARY KEY,
  tenant_id   BIGINT      NOT NULL,
  scope       VARCHAR(16) NOT NULL COMMENT '作用域: task 任务 / project 项目',
  field_key   VARCHAR(64) NOT NULL COMMENT '字段标识(租户内 scope 下唯一)',
  name        VARCHAR(64) NOT NULL COMMENT '显示名',
  type        VARCHAR(32) NOT NULL COMMENT '类型: text/number/date/select/multi_select/checkbox/user',
  options     JSON        COMMENT '选项[{value,label}]，select/multi_select 用',
  required    TINYINT     NOT NULL DEFAULT 0 COMMENT '是否必填',
  sort_no     INT         NOT NULL DEFAULT 0 COMMENT '展示排序(升序)',
  enabled     TINYINT     NOT NULL DEFAULT 1 COMMENT '是否启用',
  create_by   BIGINT,
  create_time DATETIME,
  update_by   BIGINT,
  update_time DATETIME,
  is_deleted  TINYINT     NOT NULL DEFAULT 0,
  KEY idx_scope (tenant_id, scope, enabled)
) COMMENT='自定义字段定义';

CREATE TABLE pm_field_value (
  id          BIGINT      NOT NULL PRIMARY KEY,
  tenant_id   BIGINT      NOT NULL,
  field_id    BIGINT      NOT NULL COMMENT '字段定义(pm_field_def.id)',
  entity_type VARCHAR(16) NOT NULL COMMENT '实体类型: task / project',
  entity_id   BIGINT      NOT NULL COMMENT '实体 id',
  value       TEXT        COMMENT '字段值(统一文本；多选/用户型存 JSON 字符串)',
  create_by   BIGINT,
  create_time DATETIME,
  update_by   BIGINT,
  update_time DATETIME,
  is_deleted  TINYINT     NOT NULL DEFAULT 0,
  KEY idx_entity (entity_type, entity_id),
  KEY idx_field (field_id)
) COMMENT='自定义字段值(EAV)';
