-- V60 阶段3 task→通用工作项：pm_task 追加 type_id/status_id/priority_level_id（可空，仅追加）。
-- 见 ADR-0002：本阶段仅"建列+幂等回填+双写"，不翻转 source-of-truth，不删旧列、不改前端。
-- 回填仅对已种子元数据的租户命中（如 tenant_id=1）；未种子租户三列保持 NULL，行为不变。

ALTER TABLE pm_task
  ADD COLUMN type_id           BIGINT NULL COMMENT '工作项类型 → pm_work_item_type' AFTER status,
  ADD COLUMN status_id         BIGINT NULL COMMENT '状态 → pm_status' AFTER type_id,
  ADD COLUMN priority_level_id BIGINT NULL COMMENT '优先级档位 → pm_priority_level' AFTER status_id;

-- 回填 status_id：按租户用状态名匹配状态库
UPDATE pm_task t
  JOIN pm_status s ON s.tenant_id = t.tenant_id AND s.name = t.status AND s.is_deleted = 0
  SET t.status_id = s.id
  WHERE t.status_id IS NULL;

-- 回填 type_id：默认任务类型(code=task)
UPDATE pm_task t
  JOIN pm_work_item_type wt ON wt.tenant_id = t.tenant_id AND wt.code = 'task' AND wt.is_deleted = 0
  SET t.type_id = wt.id
  WHERE t.type_id IS NULL;

-- 回填 priority_level_id：内置优先级模式下 level_value=priority 的档位
UPDATE pm_task t
  JOIN pm_priority_mode pm ON pm.tenant_id = t.tenant_id AND pm.builtin = 1 AND pm.is_deleted = 0
  JOIN pm_priority_level pl ON pl.tenant_id = t.tenant_id AND pl.mode_id = pm.id
       AND pl.level_value = t.priority AND pl.is_deleted = 0
  SET t.priority_level_id = pl.id
  WHERE t.priority_level_id IS NULL;
