-- 视图设计器：pm_view 增加命名与项目绑定列（config 仍仅承载查询配置，见 ViewConfig）。
-- name：视图名（ViewSwitcher 展示）；project_id：项目级视图所属项目（个人视图为空）。
ALTER TABLE pm_view
  ADD COLUMN name VARCHAR(64) NULL AFTER type,
  ADD COLUMN project_id BIGINT NULL AFTER name,
  ADD KEY idx_view_scope_owner (scope, owner_id),
  ADD KEY idx_view_project (project_id);
