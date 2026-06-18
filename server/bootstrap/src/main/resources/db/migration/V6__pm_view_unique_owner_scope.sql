-- 工作台/视图配置：保证每个用户每个 scope 至多一行，杜绝并发 upsert 产生重复行
-- pm_view 多租户：唯一键带 tenant_id，避免跨租户冲突
ALTER TABLE pm_view
  ADD UNIQUE KEY uk_view_owner_scope (tenant_id, owner_id, scope);
