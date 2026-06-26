-- V51 租户级操作日志合规增强：为 sys_audit_log 补充 IP / User-Agent / 模块维度，
-- 支撑管理后台「操作日志」查询（按操作人/模块/动作/时间段过滤）与合规审计要求。
-- 仅追加列与索引，不改动 V1/V5 已发布列（id/tenant_id/user_id/action/target/target_id/detail/create_time）。

ALTER TABLE sys_audit_log
  ADD COLUMN module VARCHAR(64) NULL COMMENT '功能模块（permission/member/config/project/task 等，便于分组过滤）' AFTER action,
  ADD COLUMN ip VARCHAR(64) NULL COMMENT '操作来源 IP（best-effort，X-Forwarded-For 优先）' AFTER detail,
  ADD COLUMN user_agent VARCHAR(256) NULL COMMENT '操作来源 User-Agent（best-effort）' AFTER ip;

-- 管理后台查询模式：WHERE tenant_id=? [AND module=?][AND action=?][AND user_id=?] ORDER BY id DESC
ALTER TABLE sys_audit_log
  ADD KEY idx_audit_query (tenant_id, module, action, id);
