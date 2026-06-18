-- V13 测试便利：把现有种子账号密码统一重置为「账号名 + 123」，便于本地联调与手测。
-- 规则：admin→admin123 / deptlead→deptlead123 / pmo→pmo123 / vp→vp123 / gm→gm123 / coord→coord123 / committee→committee123。
-- 密码为 BCrypt 哈希（$2b$10$，Spring BCryptPasswordEncoder 可校验），明文仅见本注释，便于测试。
-- 生产环境务必改密码或禁用演示账号。

UPDATE sys_user SET password = '$2b$10$0ohO8vidMn/3jEw.c44nEOIQeuwLLQXOUiKippjiJK2QN6z1roF4.' WHERE tenant_id = 1 AND username = 'admin';
UPDATE sys_user SET password = '$2b$10$1CguK369tFv82ZihX0IxuuYTmy0rb0pIs1ShBNCgBtHso9ZNBmaVS' WHERE tenant_id = 1 AND username = 'deptlead';
UPDATE sys_user SET password = '$2b$10$zMer87DzJOaWcZQkuY0QeudGDvHGDhlSrxNEStalY6XQ7mb6ugqeK' WHERE tenant_id = 1 AND username = 'pmo';
UPDATE sys_user SET password = '$2b$10$Rbg5lgl3SSk1hPdCs6Ftm.7N8jatl0Fn.Qih2bZYd3pPruWCrEw3.' WHERE tenant_id = 1 AND username = 'vp';
UPDATE sys_user SET password = '$2b$10$/1ljFh8nXoKsqbGTZ5KFC.2juu5laYUAejYLdbjao2GHTmrvpk/wi' WHERE tenant_id = 1 AND username = 'gm';
UPDATE sys_user SET password = '$2b$10$VBlOP4Z/71Chdr7g9j6TSed.owPyNijb8cbWrnKhNhOrQZyHw8EjS' WHERE tenant_id = 1 AND username = 'coord';
UPDATE sys_user SET password = '$2b$10$cssCdVE4Y5uPGz025aOJ2.mxONsgCOQ4PwIPhI0N5bV3ZqUFDs.Te' WHERE tenant_id = 1 AND username = 'committee';
