-- V74 订阅不变量 DB 兜底（P1-1）：每租户至多一条 active 订阅。
-- 用生成列(status=active 时取 tenant_id 否则 NULL) + 唯一索引；NULL 不参与唯一，cancelled/expired 不冲突。
-- 并发绑定时第二个 insert 触发唯一冲突，由服务层转友好提示。

ALTER TABLE sys_tenant_subscription
  ADD COLUMN active_tenant_id BIGINT
    GENERATED ALWAYS AS (CASE WHEN status = 'active' THEN tenant_id ELSE NULL END) VIRTUAL
    COMMENT '生成列：status=active 时为 tenant_id 否则 NULL，用于唯一约束',
  ADD UNIQUE KEY uk_sub_active_tenant (active_tenant_id);
