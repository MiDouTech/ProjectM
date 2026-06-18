-- V11 账号体系：新增手机号作为登录标识（全局唯一）。
-- 阶段一策略：手机号与用户名「双登录」并存；新建成员以手机号为账号，用户名缺省取手机号。
-- 历史账号（如内置 admin）手机号可留空、仍可用用户名登录；回填内置 admin 占位号便于手机号登录联调。
ALTER TABLE sys_user ADD COLUMN phone VARCHAR(20) NULL COMMENT '手机号(登录账号,全局唯一)' AFTER username;

-- 内置管理员回填占位手机号（生产环境请改）
UPDATE sys_user SET phone = '13800000000' WHERE id = 1 AND phone IS NULL;

-- 全局唯一索引：MySQL 唯一索引允许多个 NULL，历史未设手机号的账号互不冲突
CREATE UNIQUE INDEX uk_user_phone ON sys_user (phone);
