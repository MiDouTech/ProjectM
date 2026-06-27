-- V68 可配置工作区导航（ADR-0003 · L1）：租户对一级模块顶部导航的编排结果。
-- 空配置→后端回落内置默认清单（fail-safe，零破坏）。组件库 catalog 阶段一由代码内置。

CREATE TABLE pm_module_nav (
  id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
  module VARCHAR(32) NOT NULL,               -- 一级模块：project/goal/approval/report/doc/calendar/briefing
  component_code VARCHAR(32) NOT NULL,       -- 组件编码（对齐内置 catalog）
  parent_code VARCHAR(32),                   -- 父组件编码（空=二级菜单，非空=挂父下的三级）
  display_name VARCHAR(64),                  -- 改名（空=用 catalog 默认名）
  icon VARCHAR(32),                          -- 图标（空=用 catalog 默认）
  sort INT DEFAULT 0,                        -- 排序（小在前）
  enabled TINYINT DEFAULT 1,                 -- 1启用/0隐藏
  create_by   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT  DEFAULT 0,
  KEY idx_tenant_module(tenant_id, module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一级模块导航编排（可配置工作区导航）';
