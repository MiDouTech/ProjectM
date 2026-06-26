-- V67 加宽 pm_view.scope 至 32：为「表头列偏好」预留 scope 命名空间 cols:<listKey>
-- （每用户每列表一行，复用既有唯一键 uk_view_owner_scope）。纯加宽，向后兼容。

ALTER TABLE pm_view
  MODIFY COLUMN scope VARCHAR(32);
