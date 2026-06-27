-- V75 收入台账对账增强（P2-1）：关联订阅 + 多币种。
--   subscription_id：创建收入时自动关联该租户当前生效订阅（可空，历史/无订阅时为空）。
--   currency：币种，默认 CNY（阶段一线下多为人民币）。

ALTER TABLE sys_revenue_record
  ADD COLUMN subscription_id BIGINT      NULL                COMMENT '关联订阅 sys_tenant_subscription.id(可空)' AFTER tenant_id,
  ADD COLUMN currency        VARCHAR(8)  NOT NULL DEFAULT 'CNY' COMMENT '币种' AFTER amount,
  ADD KEY idx_rev_subscription (subscription_id);
