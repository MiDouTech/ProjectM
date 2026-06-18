-- V5 活动日志：为既有 sys_audit_log 增加实体维度，支撑 GET /{entity}/{id}/activities。
-- 仅追加列与索引，不改动 V1 已发布列（id/tenant_id/user_id/action/target/detail/create_time）。
-- 约定：target 复用为实体类型（project/task），target_id 为实体主键。

ALTER TABLE sys_audit_log
  ADD COLUMN target_id BIGINT NULL COMMENT '实体ID（配合 target 定位被审计实体）' AFTER target;

-- 活动流查询模式：WHERE tenant_id=? AND target=? AND target_id=? ORDER BY id DESC
ALTER TABLE sys_audit_log
  ADD KEY idx_audit_target (tenant_id, target, target_id, id);
