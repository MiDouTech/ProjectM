-- V29 多租户登录隔离（P1）：把手机号唯一约束从【全局唯一】改为【租户内唯一】。
--   每租户独立用户命名空间：同一手机号可在不同租户各有账号，登录时按租户编码 + 账号定位。
--   用户名唯一性由业务层在租户上下文内校验（sys_user 仅 idx_uname 普通索引，无需全局唯一索引）。
--   现存数据均属 tenant_id=1，改造无冲突。
ALTER TABLE sys_user DROP INDEX uk_user_phone;
CREATE UNIQUE INDEX uk_user_tenant_phone ON sys_user (tenant_id, phone);
