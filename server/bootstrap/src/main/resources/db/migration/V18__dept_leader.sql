-- V18 部门负责人：sys_dept 增加 leader_id，支撑审批流「动态审批人」中
-- 「部门主管（逐级）」与「发起人直属上级」的解析（P2）。
-- 解析逻辑：发起人所在部门负责人=直属上级；沿 parent_id 上溯取各级部门负责人。

ALTER TABLE sys_dept
  ADD COLUMN leader_id BIGINT COMMENT '部门负责人用户ID' AFTER parent_id;

-- 回填：演示数据「总部(id=1)」负责人指向占位用户「部门负责人(id=10)」（V4 种子）。
UPDATE sys_dept SET leader_id = 10 WHERE id = 1 AND tenant_id = 1;
