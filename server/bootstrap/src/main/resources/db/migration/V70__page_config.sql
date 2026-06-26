-- V70 可配置页面表单（ADR-0004 · L3.0）：租户对某实体某页面模板的字段编排。
-- 空配置→后端/前端回落默认（内置字段全显示），fail-safe；config 形如
-- {layout:{columns:2}, fields:[{fieldKey,source,group,required,readonly,width,sort}]}。

CREATE TABLE pm_page_config (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  target VARCHAR(32) NOT NULL,               -- 实体目标：task/project ...
  template_type VARCHAR(16) NOT NULL,        -- form/detail/list
  config JSON,
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  UNIQUE KEY uk_page (tenant_id, target, template_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='页面表单配置（可配置页面）';
