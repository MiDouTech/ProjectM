-- V4 立项审批：默认审批流（architecture-overview §3 的"默认审批流"列）+ 占位审批人账号。
-- 审批人账号密码均为 admin123（与 admin 同一 BCrypt 哈希），便于 e2e；生产由真实组织/角色解析替换。
-- 首节点带 JOB_LEVEL guard（npss-rule §8：S→L3+/O→L2+/I 不限），按项目 Leader 职级校验。

-- ===== 占位审批人 =====
INSERT INTO sys_user (id, tenant_id, username, name, password, dept_id, job_level, status, is_deleted) VALUES
 (10, 1, 'deptlead', '部门负责人', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', 1, 'L2', 'active', 0),
 (11, 1, 'pmo',      'PMO',       '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', 1, 'L3', 'active', 0),
 (12, 1, 'vp',       '分管副总',   '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', 1, 'L4', 'active', 0),
 (13, 1, 'gm',       '总经理',     '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', 1, 'L4', 'active', 0),
 (14, 1, 'coord',    '被协同部门', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', 1, 'L2', 'active', 0),
 (15, 1, 'committee','管委会',     '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', 1, 'L3', 'active', 0);

-- ===== 默认审批流（name = 模板 config.approvalFlow 标识，biz_type=project_init）=====
INSERT INTO approval_flow (id, tenant_id, name, biz_type, mode, definition, is_deleted)
VALUES (1, 1, 'S_STANDARD', 'project_init', 'fixed',
'{"nodes":[{"key":"dept_lead","name":"部门负责人","approvers":[10],"mode":"or","guard":"JOB_LEVEL"},{"key":"pmo","name":"PMO","approvers":[11],"mode":"or"},{"key":"vp","name":"分管副总","approvers":[12],"mode":"or"},{"key":"gm","name":"总经理","approvers":[13],"mode":"or"}]}',
0);

INSERT INTO approval_flow (id, tenant_id, name, biz_type, mode, definition, is_deleted)
VALUES (2, 1, 'I_POC', 'project_init', 'fixed',
'{"nodes":[{"key":"dept_lead","name":"部门负责人","approvers":[10],"mode":"or","guard":"JOB_LEVEL"},{"key":"pmo","name":"PMO","approvers":[11],"mode":"or"}]}',
0);

INSERT INTO approval_flow (id, tenant_id, name, biz_type, mode, definition, is_deleted)
VALUES (3, 1, 'O_NORMAL', 'project_init', 'fixed',
'{"nodes":[{"key":"dept_lead","name":"部门负责人","approvers":[10],"mode":"or","guard":"JOB_LEVEL"},{"key":"pmo","name":"PMO","approvers":[11],"mode":"or"}]}',
0);

INSERT INTO approval_flow (id, tenant_id, name, biz_type, mode, definition, is_deleted)
VALUES (4, 1, 'O_RECTIFY', 'project_init', 'fixed',
'{"nodes":[{"key":"coord","name":"被协同部门确认","approvers":[14],"mode":"or","guard":"JOB_LEVEL"},{"key":"pmo","name":"PMO","approvers":[11],"mode":"or"}]}',
0);

INSERT INTO approval_flow (id, tenant_id, name, biz_type, mode, definition, is_deleted)
VALUES (5, 1, 'O_SUPERVISE', 'project_init', 'fixed',
'{"nodes":[{"key":"committee","name":"管委会指派","approvers":[15],"mode":"or","guard":"JOB_LEVEL"},{"key":"pmo","name":"PMO","approvers":[11],"mode":"or"}]}',
0);
