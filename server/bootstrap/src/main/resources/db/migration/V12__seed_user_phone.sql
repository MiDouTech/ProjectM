-- V12 历史账号回填手机号：V11 加了 phone 列但未回填，导致内置审批人/演示账号 phone 为空，
-- 前端编辑表单 phone 必填校验卡住、无法保存。此处为空 phone 的账号回填固定占位号（生产环境请改）。
-- 占位号与 admin(13800000000) 不冲突；uk_user_phone 全局唯一，以下号码互不重复。
-- 仅在 phone IS NULL 时回填，已设置过的账号不动，可重复执行安全。

-- 内置审批人/演示账号（V4 种子，id 10~15）
UPDATE sys_user SET phone = '13800138010' WHERE id = 10 AND phone IS NULL; -- deptlead 部门负责人
UPDATE sys_user SET phone = '13800138011' WHERE id = 11 AND phone IS NULL; -- pmo
UPDATE sys_user SET phone = '13800138012' WHERE id = 12 AND phone IS NULL; -- vp 分管副总
UPDATE sys_user SET phone = '13800138013' WHERE id = 13 AND phone IS NULL; -- gm 总经理
UPDATE sys_user SET phone = '13800138014' WHERE id = 14 AND phone IS NULL; -- coord 被协同部门
UPDATE sys_user SET phone = '13800138015' WHERE id = 15 AND phone IS NULL; -- committee 管委会

-- 运行时创建的演示账号「小雅」：用户名本就是手机号，直接对齐到 phone 列
UPDATE sys_user SET phone = '13800138000' WHERE username = '13800138000' AND phone IS NULL;
