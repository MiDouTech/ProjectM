-- V63 翻转读方收尾·一致性再回填：对仍为空的 pm_task.type_id/status_id/priority_level_id 再回填一次。
-- 幂等：仅填 NULL 行(WHERE ... IS NULL)，捕获 V60 之后、双写覆盖前经其他路径(模板/历史)生成的行。
-- 不改非空值、不删列；与 V60 同口径（仅已种子元数据的租户命中）。

UPDATE pm_task t
  JOIN pm_status s ON s.tenant_id = t.tenant_id AND s.name = t.status AND s.is_deleted = 0
  SET t.status_id = s.id
  WHERE t.status_id IS NULL;

UPDATE pm_task t
  JOIN pm_work_item_type wt ON wt.tenant_id = t.tenant_id AND wt.code = 'task' AND wt.is_deleted = 0
  SET t.type_id = wt.id
  WHERE t.type_id IS NULL;

UPDATE pm_task t
  JOIN pm_priority_mode pm ON pm.tenant_id = t.tenant_id AND pm.builtin = 1 AND pm.is_deleted = 0
  JOIN pm_priority_level pl ON pl.tenant_id = t.tenant_id AND pl.mode_id = pm.id
       AND pl.level_value = t.priority AND pl.is_deleted = 0
  SET t.priority_level_id = pl.id
  WHERE t.priority_level_id IS NULL;
