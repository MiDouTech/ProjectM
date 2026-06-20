-- V36 修复平台超管初始密码。
-- 背景：V28 平台种子误用了与租户 admin 相同的 BCrypt 哈希（该哈希对应明文 admin123），
--       导致 superadmin 无法用约定口令 superadmin123 登录运营平台。
-- 处理：仅当密码仍为那串错误哈希时，更新为 superadmin123 的正确 BCrypt（$2a$10$）哈希，
--       避免覆盖运维可能已手动修改过的口令。生产环境请另行轮换。
UPDATE sys_platform_admin
SET password = '$2a$10$yJ7EbOYVM6BXnWNbnZTPSusK25s6dvARL7AK33xgv5oemlZNODk6i'
WHERE username = 'superadmin'
  AND password = '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m';
