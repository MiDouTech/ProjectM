-- V76 运营审计表 append-only 强制（P2-2）：DB 触发器禁止 UPDATE/DELETE，防篡改。
-- 审计仅允许 INSERT 与 SELECT；任何更新/删除尝试报错回滚（强于应用层约定）。
-- 单语句触发器(SIGNAL)，无需 DELIMITER。

CREATE TRIGGER trg_audit_no_update BEFORE UPDATE ON sys_platform_audit_log
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '运营审计为追加写，禁止修改';

CREATE TRIGGER trg_audit_no_delete BEFORE DELETE ON sys_platform_audit_log
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '运营审计为追加写，禁止删除';
