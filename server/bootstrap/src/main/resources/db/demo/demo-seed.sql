-- =============================================================================
-- 米多 PM 演示数据（demo-seed）—— 团队演示 / 联调用，非生产数据
-- -----------------------------------------------------------------------------
-- 用途：把空系统填充为一套成体系的仿真数据，覆盖各模块与页面。
-- 落点：db/demo（不在 Flyway 默认 db/migration 路径，生产不会自动加载）。
-- 执行：见 README「演示数据」一节，或 scripts/load-demo.sh。
-- 幂等：所有演示行使用 id 段 [9000000, 9999999]；脚本先按该段清理再插入，可反复重灌、便于演示复位。
-- 租户：业务数据全部挂在内置租户 tenant_id=1（演示登录 admin/admin123）。
--      平台运营后台另造若干演示租户（仅用于「租户/套餐/收入」列表展示，无业务数据）。
-- 演示员工密码统一为 admin123（与内置 admin 同一 BCrypt 哈希）。
-- 状态/枚举一律取自代码事实源：项目/任务/费用状态存中文 code，干系人角色等存英文 code。
-- =============================================================================

SET @T := 1;        -- 业务租户 id
SET @U := 1;        -- 操作人（admin）

-- ============================ 0. 幂等清理（id 段 9000000~9999999） =============
DELETE FROM pm_notification          WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_comment               WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_briefing_recipient    WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_briefing              WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_briefing_template     WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_schedule_participant  WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_schedule              WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_calendar              WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_doc_version           WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_doc                   WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_cost                  WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_change_request        WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM approval_task            WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM approval_instance        WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_npss_score            WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_npss_subject_member   WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_npss_subject          WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_npss_subject_template WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_npss_review           WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_stakeholder           WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_goal_alignment        WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_goal                  WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_work_hour             WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_task                  WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_project_member        WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM pm_project               WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM sys_user_role            WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM sys_user                 WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM sys_dept                 WHERE id BETWEEN 9000000 AND 9999999;
-- 平台域（无 tenant_id）
DELETE FROM sys_tenant_quota_usage   WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM sys_revenue_record       WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM sys_announcement         WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM sys_tenant_subscription  WHERE id BETWEEN 9000000 AND 9999999;
DELETE FROM sys_tenant               WHERE id BETWEEN 9000000 AND 9999999;

-- ============================ 1. 组织架构：部门 / 成员 / 角色 ==================
-- 部门树（挂在内置「总部」id=1 之下）
INSERT INTO sys_dept (id, tenant_id, name, parent_id, is_deleted) VALUES
  (9001001, @T, '产品研发中心', 1,       0),
  (9001002, @T, '市场品牌部',   1,       0),
  (9001003, @T, '运营中心',     1,       0),
  (9001004, @T, '职能支持部',   1,       0),
  (9001005, @T, '前端组',       9001001, 0),
  (9001006, @T, '后端组',       9001001, 0),
  (9001007, @T, '测试组',       9001001, 0),
  (9001008, @T, '客户成功组',   9001003, 0);

-- 成员（演示员工，密码统一 admin123；职级 L1~L5；status 必填=active）
INSERT INTO sys_user (id, tenant_id, username, name, password, phone, dept_id, job_level, status, is_deleted) VALUES
  (9002001, @T, 'zhangmy',  '张明远', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', '13800010001', 9001001, 'L4', 'active', 0),
  (9002002, @T, 'lisy',     '李思颖', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', '13800010002', 9001005, 'L3', 'active', 0),
  (9002003, @T, 'wanghr',   '王浩然', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', '13800010003', 9001006, 'L3', 'active', 0),
  (9002004, @T, 'chenjy',   '陈佳怡', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', '13800010004', 9001007, 'L3', 'active', 0),
  (9002005, @T, 'liuyh',    '刘宇航', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', '13800010005', 9001005, 'L2', 'active', 0),
  (9002006, @T, 'zhaoxy',   '赵雪莹', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', '13800010006', 9001005, 'L2', 'active', 0),
  (9002007, @T, 'sunqh',    '孙启航', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', '13800010007', 9001006, 'L2', 'active', 0),
  (9002008, @T, 'zhouwb',   '周文博', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', '13800010008', 9001006, 'L2', 'active', 0),
  (9002009, @T, 'wumq',     '吴梦琪', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', '13800010009', 9001007, 'L2', 'active', 0),
  (9002010, @T, 'zhengkw',  '郑凯文', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', '13800010010', 9001002, 'L4', 'active', 0),
  (9002011, @T, 'fengln',   '冯丽娜', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', '13800010011', 9001002, 'L3', 'active', 0),
  (9002012, @T, 'hejj',     '何俊杰', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', '13800010012', 9001003, 'L4', 'active', 0),
  (9002013, @T, 'dengyt',   '邓雅婷', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', '13800010013', 9001003, 'L3', 'active', 0),
  (9002014, @T, 'xuzq',     '许志强', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', '13800010014', 9001008, 'L2', 'active', 0),
  (9002015, @T, 'zengxl',   '曾晓琳', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', '13800010015', 9001004, 'L3', 'active', 0),
  (9002016, @T, 'luozy',    '罗振宇', '$2a$10$yjoI4ezno4WqUdzpz3y2feVr6SVkb0WH1NFGxm93uDpz9K.o7nn7m', '13800010016', 9001004, 'L3', 'active', 0);

-- 演示员工统一授予「超级管理员」角色（role=1），便于演示时任意账号登录都能看到数据
INSERT INTO sys_user_role (id, tenant_id, user_id, role_id, is_deleted) VALUES
  (9002001, @T, 9002001, 1, 0), (9002002, @T, 9002002, 1, 0), (9002003, @T, 9002003, 1, 0),
  (9002004, @T, 9002004, 1, 0), (9002005, @T, 9002005, 1, 0), (9002006, @T, 9002006, 1, 0),
  (9002007, @T, 9002007, 1, 0), (9002008, @T, 9002008, 1, 0), (9002009, @T, 9002009, 1, 0),
  (9002010, @T, 9002010, 1, 0), (9002011, @T, 9002011, 1, 0), (9002012, @T, 9002012, 1, 0),
  (9002013, @T, 9002013, 1, 0), (9002014, @T, 9002014, 1, 0), (9002015, @T, 9002015, 1, 0),
  (9002016, @T, 9002016, 1, 0);

-- 部门负责人
UPDATE sys_dept SET leader_id = 9002001 WHERE id = 9001001;
UPDATE sys_dept SET leader_id = 9002010 WHERE id = 9001002;
UPDATE sys_dept SET leader_id = 9002012 WHERE id = 9001003;
UPDATE sys_dept SET leader_id = 9002015 WHERE id = 9001004;
UPDATE sys_dept SET leader_id = 9002002 WHERE id = 9001005;
UPDATE sys_dept SET leader_id = 9002003 WHERE id = 9001006;
UPDATE sys_dept SET leader_id = 9002004 WHERE id = 9001007;
UPDATE sys_dept SET leader_id = 9002014 WHERE id = 9001008;

-- ============================ 2. 项目（5 条明星项目线 + 1 条审批中） ===========
-- category：S 战略级 / I 创新级 / O 运营级（sub_category 细分）
-- status（中文 code）：草稿/审批中/已注册/进行中/结果验收/已结案/价值验收中/已评价
INSERT INTO pm_project
  (id, tenant_id, code, name, description, category, sub_category, leader_id, status, start_date, end_date, budget, actual_cost, value_review_due_date, create_by, create_time, is_deleted) VALUES
  (9003001, @T, 'PRJ-2026-001', '米多 PM 系统自研',     '对标 Worktile 的通用项目管理系统，差异化护城河：立项审批 + 干系人 + NPSS 验收。', 'I', NULL,       9002001, '进行中',     '2026-01-06', '2026-09-30', 1800000.00, 760000.00, '2026-10-15', @U, '2026-01-06 09:00:00', 0),
  (9003002, @T, 'PRJ-2026-002', '2026 品牌官网改版',     '官网视觉与转化链路全面升级，目标转化率提升至 5%。',                       'S', NULL,       9002010, '已评价',     '2026-02-01', '2026-05-20', 600000.00,  548000.00, '2026-06-01', @U, '2026-02-01 09:00:00', 0),
  (9003003, @T, 'PRJ-2026-003', '华东区渠道拓展',         '华东重点城市渠道铺设与经销商招募。',                                       'O', '常规运营', 9002012, '进行中',     '2026-03-01', '2026-12-31', 1200000.00, 410000.00, NULL,        @U, '2026-03-01 09:00:00', 0),
  (9003004, @T, 'PRJ-2026-004', '客服质量专项整改',       '针对 Q1 客诉率偏高的定向整改专项。',                                       'O', '定向整改', 9002013, '结果验收',   '2026-04-01', '2026-06-30', 200000.00,  165000.00, NULL,        @U, '2026-04-01 09:00:00', 0),
  (9003005, @T, 'PRJ-2026-005', '数据合规专项督办',       '个人信息保护合规整改，管委会专项督办。',                                   'O', '专项督办', 9002016, '已结案',     '2026-01-15', '2026-04-30', 150000.00,  142000.00, NULL,        @U, '2026-01-15 09:00:00', 0),
  (9003006, @T, 'PRJ-2026-006', '移动端 App 一期',        '面向一线业务的移动端 App，立项审批中。',                                   'I', NULL,       9002003, '审批中',     '2026-07-01', '2026-12-15', 900000.00,  NULL,      '2026-12-30', @U, '2026-06-20 09:00:00', 0);

-- 项目成员（project_role 中文：管理员/普通成员/只读成员）
INSERT INTO pm_project_member (id, tenant_id, project_id, user_id, project_role, is_deleted) VALUES
  (9003101, @T, 9003001, 9002001, '管理员',   0),
  (9003102, @T, 9003001, 9002002, '普通成员', 0),
  (9003103, @T, 9003001, 9002003, '普通成员', 0),
  (9003104, @T, 9003001, 9002005, '普通成员', 0),
  (9003105, @T, 9003001, 9002007, '普通成员', 0),
  (9003106, @T, 9003001, 9002009, '普通成员', 0),
  (9003107, @T, 9003002, 9002010, '管理员',   0),
  (9003108, @T, 9003002, 9002011, '普通成员', 0),
  (9003109, @T, 9003002, 9002006, '普通成员', 0),
  (9003110, @T, 9003003, 9002012, '管理员',   0),
  (9003111, @T, 9003003, 9002013, '普通成员', 0),
  (9003112, @T, 9003003, 9002014, '普通成员', 0),
  (9003113, @T, 9003004, 9002013, '管理员',   0),
  (9003114, @T, 9003004, 9002014, '普通成员', 0),
  (9003115, @T, 9003005, 9002016, '管理员',   0),
  (9003116, @T, 9003005, 9002015, '只读成员', 0),
  (9003117, @T, 9003006, 9002003, '管理员',   0),
  (9003118, @T, 9003006, 9002008, '普通成员', 0);

-- ============================ 3. 任务（看板/列表/优先级/工时） ================
-- status（中文）：未开始/进行中/已完成/已验收  priority：1高/2中/3低
INSERT INTO pm_task
  (id, tenant_id, project_id, parent_id, title, description, assignee_id, status, priority, stage, start_date, due_date, is_milestone, est_hours, actual_hours, create_by, create_time, is_deleted) VALUES
  -- 项目 9003001 米多 PM 系统
  (9004001, @T, 9003001, 0, '需求梳理与 PRD 评审',     '汇总各模块需求，输出 PRD 并评审定稿。',     9002001, '已验收', 1, '需求', '2026-01-06', '2026-01-20', 1, 40.0, 38.0, @U, '2026-01-06 10:00:00', 0),
  (9004002, @T, 9003001, 0, '数据库 DDL 与多租户设计',  '完成核心表 DDL 与租户隔离方案。',           9002003, '已完成', 1, '设计', '2026-01-21', '2026-02-10', 1, 60.0, 64.0, @U, '2026-01-21 10:00:00', 0),
  (9004003, @T, 9003001, 0, '项目/任务模块后端',        '项目、任务、看板视图后端实现。',           9002007, '进行中', 1, '开发', '2026-02-11', '2026-04-30', 0, 160.0, 120.0, @U, '2026-02-11 10:00:00', 0),
  (9004004, @T, 9003001, 0, '前端工作台与项目列表',      '工作台聚合视图与项目列表页。',             9002002, '进行中', 2, '开发', '2026-02-11', '2026-04-30', 0, 140.0, 96.0, @U, '2026-02-11 10:00:00', 0),
  (9004005, @T, 9003001, 0, 'NPSS 验收引擎',           '两段式价值验收算分与奖金硬校验。',         9002008, '未开始', 1, '开发', '2026-05-06', '2026-06-20', 0, 80.0, NULL, @U, '2026-04-25 10:00:00', 0),
  (9004006, @T, 9003001, 0, '回归测试与联调',          '全模块联调与回归测试。',                   9002009, '未开始', 2, '测试', '2026-06-23', '2026-08-31', 0, 100.0, NULL, @U, '2026-04-25 10:00:00', 0),
  (9004007, @T, 9003001, 9004004, '看板拖拽交互',       '看板列拖拽与状态流转。',                   9002005, '进行中', 3, '开发', '2026-03-01', '2026-03-20', 0, 24.0, 16.0, @U, '2026-03-01 10:00:00', 0),
  -- 项目 9003002 官网改版
  (9004011, @T, 9003002, 0, '视觉风格定稿',            '品牌视觉与设计系统定稿。',                 9002011, '已验收', 1, '设计', '2026-02-01', '2026-02-20', 1, 48.0, 50.0, @U, '2026-02-01 10:00:00', 0),
  (9004012, @T, 9003002, 0, '官网前端开发',            '响应式官网页面开发。',                     9002006, '已验收', 1, '开发', '2026-02-21', '2026-04-10', 0, 120.0, 118.0, @U, '2026-02-21 10:00:00', 0),
  (9004013, @T, 9003002, 0, '转化漏斗埋点',            '关键转化路径埋点与数据看板。',             9002007, '已完成', 2, '开发', '2026-04-11', '2026-04-30', 0, 32.0, 30.0, @U, '2026-04-11 10:00:00', 0),
  (9004014, @T, 9003002, 0, '上线与灰度验证',          '正式上线并灰度观测转化。',                 9002010, '已验收', 1, '上线', '2026-05-06', '2026-05-20', 1, 24.0, 22.0, @U, '2026-05-01 10:00:00', 0),
  -- 项目 9003003 华东渠道
  (9004021, @T, 9003003, 0, '重点城市选址调研',        '上海/杭州/南京渠道选址调研。',             9002013, '已完成', 2, '调研', '2026-03-01', '2026-03-31', 0, 60.0, 58.0, @U, '2026-03-01 10:00:00', 0),
  (9004022, @T, 9003003, 0, '经销商招募',              '目标 20 家经销商签约。',                   9002014, '进行中', 1, '执行', '2026-04-01', '2026-09-30', 0, 200.0, 90.0, @U, '2026-04-01 10:00:00', 0),
  (9004023, @T, 9003003, 0, '渠道培训体系搭建',        '经销商培训课程与认证。',                   9002013, '未开始', 3, '执行', '2026-07-01', '2026-10-31', 0, 80.0, NULL, @U, '2026-04-01 10:00:00', 0),
  -- 项目 9003004 客服整改
  (9004031, @T, 9003004, 0, '客诉根因分析',            '梳理 Q1 客诉 TOP 问题根因。',              9002014, '已完成', 1, '分析', '2026-04-01', '2026-04-15', 1, 32.0, 34.0, @U, '2026-04-01 10:00:00', 0),
  (9004032, @T, 9003004, 0, 'SOP 优化与培训',          '修订服务 SOP 并完成全员培训。',            9002013, '已完成', 2, '执行', '2026-04-16', '2026-05-31', 0, 48.0, 46.0, @U, '2026-04-16 10:00:00', 0),
  (9004033, @T, 9003004, 0, '整改效果验收',            '复测客诉率达成整改目标。',                 9002013, '进行中', 1, '验收', '2026-06-01', '2026-06-30', 1, 16.0, 8.0, @U, '2026-06-01 10:00:00', 0),
  -- 项目 9003005 合规督办
  (9004041, @T, 9003005, 0, '合规差距评估',            '对照个保法梳理合规差距。',                 9002016, '已验收', 1, '评估', '2026-01-15', '2026-02-10', 1, 40.0, 42.0, @U, '2026-01-15 10:00:00', 0),
  (9004042, @T, 9003005, 0, '隐私政策与授权改造',      '更新隐私政策与用户授权链路。',             9002015, '已验收', 1, '整改', '2026-02-11', '2026-03-31', 0, 60.0, 58.0, @U, '2026-02-11 10:00:00', 0),
  (9004043, @T, 9003005, 0, '管委会督办结案',          '专项督办结案评审。',                       9002016, '已验收', 2, '结案', '2026-04-01', '2026-04-30', 1, 16.0, 16.0, @U, '2026-04-01 10:00:00', 0),
  -- 项目 9003006 移动端（审批中，少量未开始）
  (9004051, @T, 9003006, 0, '技术选型与架构',          '移动端技术栈与架构方案。',                 9002003, '未开始', 1, '设计', '2026-07-01', '2026-07-20', 0, 40.0, NULL, @U, '2026-06-20 10:00:00', 0),
  (9004052, @T, 9003006, 0, 'MVP 功能清单',            '一期 MVP 功能范围确认。',                   9002008, '未开始', 2, '需求', '2026-07-01', '2026-07-15', 0, 24.0, NULL, @U, '2026-06-20 10:00:00', 0);

-- 工时填报
INSERT INTO pm_work_hour (id, tenant_id, task_id, user_id, kind, category, work_date, hours, remark, is_deleted) VALUES
  (9004501, @T, 9004003, 9002007, '开发', '工资', '2026-03-02', 8.0, '项目模块接口开发', 0),
  (9004502, @T, 9004003, 9002007, '开发', '工资', '2026-03-03', 7.5, '任务看板接口', 0),
  (9004503, @T, 9004004, 9002002, '开发', '工资', '2026-03-02', 8.0, '工作台联调', 0),
  (9004504, @T, 9004004, 9002005, '开发', '工资', '2026-03-04', 6.0, '看板拖拽组件', 0),
  (9004505, @T, 9004012, 9002006, '开发', '工资', '2026-03-10', 8.0, '官网首页开发', 0),
  (9004506, @T, 9004013, 9002007, '开发', '工资', '2026-04-12', 5.0, '埋点接入', 0),
  (9004507, @T, 9004022, 9002014, '会议', '工资', '2026-04-20', 3.0, '经销商洽谈', 0),
  (9004508, @T, 9004031, 9002014, '文档', '工资', '2026-04-05', 6.0, '客诉根因报告', 0),
  (9004509, @T, 9004001, 9002001, '会议', '工资', '2026-01-15', 4.0, 'PRD 评审会', 0),
  (9004510, @T, 9004011, 9002011, '设计', '工资', '2026-02-10', 8.0, '视觉稿评审', 0),
  (9004511, @T, 9004041, 9002016, '文档', '工资', '2026-01-20', 6.0, '合规差距评估报告', 0),
  (9004512, @T, 9004032, 9002013, '测试', '工资', '2026-05-20', 4.0, 'SOP 复盘', 0);

-- ============================ 4. 目标 / KR / 对齐 =============================
-- type：objective 目标 / kr 关键结果
INSERT INTO pm_goal (id, tenant_id, title, type, parent_id, owner_id, period, metric_unit, metric_start, metric_target, metric_current, progress, create_by, create_time, is_deleted) VALUES
  (9005001, @T, '2026 年公司营收增长 30%',     'objective', 0,       9002010, '2026',   NULL,  NULL,    NULL,    NULL,    45.00, @U, '2026-01-02 09:00:00', 0),
  (9005002, @T, '新签合同额达 5000 万',         'kr',        9005001, 9002012, '2026',   '万元', 0.00,   5000.00, 2150.00, 43.00, @U, '2026-01-02 09:00:00', 0),
  (9005003, @T, '官网转化率提升至 5%',          'kr',        9005001, 9002010, '2026',   '%',    2.10,   5.00,    4.30,    76.00, @U, '2026-01-02 09:00:00', 0),
  (9005004, @T, '打造行业领先的 PM 产品',       'objective', 0,       9002001, '2026',   NULL,  NULL,    NULL,    NULL,    55.00, @U, '2026-01-02 09:00:00', 0),
  (9005005, @T, '核心模块全部上线',             'kr',        9005004, 9002001, '2026',   '个',   0.00,   12.00,   8.00,    67.00, @U, '2026-01-02 09:00:00', 0),
  (9005006, @T, '内部满意度 NPS ≥ 8 分',        'kr',        9005004, 9002001, '2026',   '分',   5.50,   8.00,    7.20,    68.00, @U, '2026-01-02 09:00:00', 0);

-- 目标对齐（KR ← 项目）target_type：project / task
INSERT INTO pm_goal_alignment (id, tenant_id, goal_id, target_type, target_id, weight, is_deleted) VALUES
  (9005101, @T, 9005003, 'project', 9003002, 1.00, 0),
  (9005102, @T, 9005005, 'project', 9003001, 1.00, 0),
  (9005103, @T, 9005002, 'project', 9003003, 1.00, 0);

-- ============================ 5. 干系人（影响–态度矩阵） ======================
-- role：sponsor/business/team/finance/regulator/other  category：internal/external
-- power_level/interest_level：1~5
INSERT INTO pm_stakeholder (id, tenant_id, project_id, user_id, external_name, role, category, power_level, interest_level, npss_weight, is_deleted) VALUES
  -- 项目 9003001
  (9006001, @T, 9003001, 9002001, NULL,         'sponsor',  'internal', 5, 5, 30.00, 0),
  (9006002, @T, 9003001, 9002012, NULL,         'business', 'internal', 4, 5, 30.00, 0),
  (9006003, @T, 9003001, 9002003, NULL,         'team',     'internal', 3, 4, 20.00, 0),
  (9006004, @T, 9003001, 9002015, NULL,         'finance',  'internal', 3, 2, 20.00, 0),
  -- 项目 9003002（官网改版，参与 NPSS 验收）
  (9006011, @T, 9003002, 9002010, NULL,         'sponsor',  'internal', 5, 5, 25.00, 0),
  (9006012, @T, 9003002, 9002012, NULL,         'business', 'internal', 4, 5, 25.00, 0),
  (9006013, @T, 9003002, 9002011, NULL,         'team',     'internal', 3, 4, 30.00, 0),
  (9006014, @T, 9003002, NULL,    '外部设计顾问', 'other',    'external', 2, 3, 20.00, 0),
  -- 项目 9003004
  (9006021, @T, 9003004, 9002013, NULL,         'sponsor',  'internal', 4, 5, 40.00, 0),
  (9006022, @T, 9003004, 9002014, NULL,         'business', 'internal', 3, 4, 30.00, 0),
  (9006023, @T, 9003004, NULL,    '重点客户代表', 'business', 'external', 4, 5, 30.00, 0),
  -- 项目 9003005
  (9006031, @T, 9003005, 9002016, NULL,         'sponsor',  'internal', 5, 5, 50.00, 0),
  (9006032, @T, 9003005, 9002015, NULL,         'finance',  'internal', 3, 3, 20.00, 0),
  (9006033, @T, 9003005, NULL,    '法务合规顾问', 'regulator','external', 4, 4, 30.00, 0);

-- ============================ 6. NPSS 两段式价值验收 =========================
-- 租户级评价主体模板（受益方合计须 ≥ 50%，启用主体合计 = 100%）
INSERT INTO pm_npss_subject_template (id, tenant_id, name, weight, beneficiary, sort, enabled, create_by, create_time, is_deleted) VALUES
  (9007401, @T, '业务方',   50.00, 1, 1, 1, @U, '2026-01-02 09:00:00', 0),
  (9007402, @T, '项目团队', 30.00, 0, 2, 1, @U, '2026-01-02 09:00:00', 0),
  (9007403, @T, '管理层',   20.00, 0, 3, 1, @U, '2026-01-02 09:00:00', 0);

-- 项目 9003002 的评价主体实例
INSERT INTO pm_npss_subject (id, tenant_id, project_id, template_id, name, weight, beneficiary, sort, create_by, create_time, is_deleted) VALUES
  (9007101, @T, 9003002, 9007401, '业务方',   50.00, 1, 1, @U, '2026-05-25 09:00:00', 0),
  (9007102, @T, 9003002, 9007402, '项目团队', 30.00, 0, 2, @U, '2026-05-25 09:00:00', 0),
  (9007103, @T, 9003002, 9007403, '管理层',   20.00, 0, 3, @U, '2026-05-25 09:00:00', 0);

-- 主体成员（绑定到 9003002 的干系人）
INSERT INTO pm_npss_subject_member (id, tenant_id, subject_id, stakeholder_id, is_deleted) VALUES
  (9007201, @T, 9007101, 9006012, 0),
  (9007202, @T, 9007101, 9006014, 0),
  (9007203, @T, 9007102, 9006013, 0),
  (9007204, @T, 9007103, 9006011, 0);

-- 评分轮次（9003002 已完成：weighted_score 86.50 → mixed）
INSERT INTO pm_npss_review (id, tenant_id, project_id, round, status, weighted_score, result_level, reviewed_at, create_by, create_time, is_deleted) VALUES
  (9007001, @T, 9003002, '2026-验收', 'done', 86.50, 'mixed', '2026-05-30 18:00:00', @U, '2026-05-25 09:00:00', 0);

-- 单干系人评分（0~10，带主体快照与权重快照）
INSERT INTO pm_npss_score (id, tenant_id, review_id, stakeholder_id, subject_id, score, weight, comment, create_by, create_time, is_deleted) VALUES
  (9007301, @T, 9007001, 9006012, 9007101, 9, 50.00, '业务目标基本达成，转化提升明显', @U, '2026-05-28 10:00:00', 0),
  (9007302, @T, 9007001, 9006014, 9007101, 8, 50.00, '设计交付质量高',                 @U, '2026-05-28 10:00:00', 0),
  (9007303, @T, 9007001, 9006013, 9007102, 9, 30.00, '团队协作顺畅',                   @U, '2026-05-28 10:00:00', 0),
  (9007304, @T, 9007001, 9006011, 9007103, 8, 20.00, '整体满意，节奏可再快',           @U, '2026-05-28 10:00:00', 0);

-- ============================ 7. 立项审批 / 审批待办 =========================
-- 复用内置审批流：flow_id=2(I_POC)、flow_id=1(S_STANDARD)、flow_id=6(COST_DEFAULT)
-- status：pending/approved/rejected/withdrawn   action：approve/reject/transfer/NULL
INSERT INTO approval_instance (id, tenant_id, flow_id, biz_type, biz_id, status, current_node, form_data, applicant_id, create_by, create_time, is_deleted) VALUES
  (9008001, @T, 2, 'project_init', 9003006, 'pending',  'pmo',  '{"name":"移动端 App 一期","category":"I"}', 9002003, 9002003, '2026-06-20 09:30:00', 0),
  (9008002, @T, 1, 'project_init', 9003002, 'approved', 'gm',   '{"name":"2026 品牌官网改版","category":"S"}', 9002010, 9002010, '2026-01-28 09:30:00', 0),
  (9008003, @T, 6, 'cost',         9009003, 'pending',  'pmo',  '{"title":"渠道地推物料制作","amount":80000}', 9002014, 9002014, '2026-04-18 09:30:00', 0);

-- 审批待办/记录（审批人复用内置占位账号 10~15：deptlead/pmo/vp/gm）
INSERT INTO approval_task (id, tenant_id, instance_id, node, approver_id, action, comment, acted_at, create_by, create_time, is_deleted) VALUES
  (9008101, @T, 9008001, 'dept_lead', 10, 'approve', '同意立项', '2026-06-20 14:00:00', @U, '2026-06-20 09:30:00', 0),
  (9008102, @T, 9008001, 'pmo',       11, NULL,      NULL,        NULL,                  @U, '2026-06-20 14:00:00', 0),
  (9008103, @T, 9008002, 'dept_lead', 10, 'approve', '同意',     '2026-01-28 11:00:00', @U, '2026-01-28 09:30:00', 0),
  (9008104, @T, 9008002, 'pmo',       11, 'approve', '预算合理', '2026-01-29 10:00:00', @U, '2026-01-28 09:30:00', 0),
  (9008105, @T, 9008002, 'gm',        13, 'approve', '批准',     '2026-01-30 16:00:00', @U, '2026-01-28 09:30:00', 0),
  (9008106, @T, 9008003, 'dept_lead', 10, 'approve', '同意',     '2026-04-18 15:00:00', @U, '2026-04-18 09:30:00', 0),
  (9008107, @T, 9008003, 'pmo',       11, NULL,      NULL,        NULL,                  @U, '2026-04-18 15:00:00', 0);

-- ============================ 8. 变更中心 ===================================
-- biz_type：goal/project/cost  status：draft/pending/approved/applied/rejected/withdrawn
INSERT INTO pm_change_request (id, tenant_id, biz_type, biz_id, change_type, title, reason, impact, status, applied_at, create_by, create_time, is_deleted) VALUES
  (9008501, @T, 'goal',    9005002, 'goal_target', '新签合同额目标 4500→5000 万', '上半年增长超预期，上调年度目标。',     '影响下半年销售节奏与激励。', 'applied',  '2026-05-10 10:00:00', 9002012, '2026-05-08 09:00:00', 0),
  (9008502, @T, 'goal',    9005003, 'goal_scope',  '官网转化口径调整',             '统一以「注册转化」为口径。',           '历史数据需重新对齐。',       'pending',  NULL,                  9002010, '2026-06-15 09:00:00', 0),
  (9008503, @T, 'project', 9003003, 'project_plan','华东渠道结项延期至年底',       '经销商招募进度滞后，申请延期。',       '里程碑顺延，预算不变。',     'approved', NULL,                  9002012, '2026-06-18 09:00:00', 0),
  (9008504, @T, 'goal',    9005006, 'goal_owner',  'NPS 目标负责人变更',           '原负责人轮岗，移交新负责人。',         '无重大影响。',               'applied',  '2026-06-12 10:00:00', 9002001, '2026-06-12 09:00:00', 0);

-- ============================ 9. 费用 / 预算 =================================
-- status（中文）：未发生/已发生/被退回
INSERT INTO pm_cost (id, tenant_id, project_id, title, account, budget_amount, actual_amount, occur_date, pay_date, status, is_deleted) VALUES
  (9009001, @T, 9003002, '官网设计外包',     '服务费', 120000.00, 118000.00, '2026-02-20', '2026-03-05', '已发生', 0),
  (9009002, @T, 9003002, '云资源与 CDN',     '服务费', 60000.00,  52000.00,  '2026-03-01', '2026-04-01', '已发生', 0),
  (9009003, @T, 9003003, '渠道地推物料制作', '制作',   80000.00,  NULL,      '2026-04-25', NULL,         '未发生', 0),
  (9009004, @T, 9003003, '华东出差差旅',     '差旅',   100000.00, 36000.00,  '2026-03-10', '2026-03-20', '已发生', 0),
  (9009005, @T, 9003003, '经销商招商会',     '会务',   150000.00, 88000.00,  '2026-05-15', '2026-05-25', '已发生', 0),
  (9009006, @T, 9003001, '研发服务器采购',   '设备',   200000.00, 180000.00, '2026-02-01', '2026-02-15', '已发生', 0),
  (9009007, @T, 9003004, '客服培训讲师费',   '服务费', 30000.00,  28000.00,  '2026-05-10', '2026-05-20', '已发生', 0),
  (9009008, @T, 9003005, '合规法律咨询',     '服务费', 50000.00,  46000.00,  '2026-02-15', '2026-03-01', '已发生', 0),
  (9009009, @T, 9003001, '团队建设活动',     '其他',   20000.00,  NULL,      '2026-07-10', NULL,         '被退回', 0);

-- ============================ 10. 文档（知识库树 + 版本） =====================
-- type：folder/doc/file
INSERT INTO pm_doc (id, tenant_id, project_id, parent_id, type, title, sort_no, current_version_id, trashed, create_by, create_time, is_deleted) VALUES
  (9010001, @T, 9003001, 0,       'folder', '需求与设计', 1, NULL,    0, @U, '2026-01-06 10:00:00', 0),
  (9010002, @T, 9003001, 9010001, 'doc',    'PM 系统 PRD', 1, 9010101, 0, @U, '2026-01-10 10:00:00', 0),
  (9010003, @T, 9003001, 9010001, 'doc',    '多租户与权限设计', 2, 9010102, 0, @U, '2026-01-22 10:00:00', 0),
  (9010004, @T, 9003001, 0,       'folder', '会议纪要', 2, NULL,    0, @U, '2026-01-06 10:00:00', 0),
  (9010005, @T, 9003001, 9010004, 'doc',    '项目启动会纪要', 1, 9010103, 0, @U, '2026-01-08 10:00:00', 0),
  (9010006, @T, 9003002, 0,       'folder', '官网改版', 1, NULL,    0, @U, '2026-02-01 10:00:00', 0),
  (9010007, @T, 9003002, 9010006, 'doc',    '改版方案与转化策略', 1, 9010104, 0, @U, '2026-02-05 10:00:00', 0),
  (9010008, @T, 9003005, 0,       'doc',    '数据合规整改报告', 1, 9010105, 0, @U, '2026-04-20 10:00:00', 0);

INSERT INTO pm_doc_version (id, tenant_id, doc_id, version_no, title, content, content_text, change_note, create_by, create_time, is_deleted) VALUES
  (9010101, @T, 9010002, 2, 'PM 系统 PRD', '{"type":"doc","content":[{"type":"heading","attrs":{"level":1},"content":[{"type":"text","text":"米多 PM 系统 PRD"}]},{"type":"paragraph","content":[{"type":"text","text":"差异化护城河：立项审批引擎 + 干系人管理 + NPSS 两段式价值验收。"}]}]}', '米多 PM 系统 PRD。差异化护城河：立项审批引擎 + 干系人管理 + NPSS 两段式价值验收。', '补充验收章节', @U, '2026-01-15 10:00:00', 0),
  (9010102, @T, 9010003, 1, '多租户与权限设计', '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"所有业务表带 tenant_id，经 MyBatis-Plus 多租户拦截器统一注入。"}]}]}', '所有业务表带 tenant_id，经多租户拦截器统一注入。', NULL, @U, '2026-01-22 10:00:00', 0),
  (9010103, @T, 9010005, 1, '项目启动会纪要', '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"参会：研发/产品/测试。结论：按 P0/P1/P2 分阶段推进。"}]}]}', '参会：研发/产品/测试。结论：按 P0/P1/P2 分阶段推进。', NULL, @U, '2026-01-08 10:00:00', 0),
  (9010104, @T, 9010007, 1, '改版方案与转化策略', '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"目标转化率提升至 5%，重构首页与注册漏斗。"}]}]}', '目标转化率提升至 5%，重构首页与注册漏斗。', NULL, @U, '2026-02-05 10:00:00', 0),
  (9010105, @T, 9010008, 1, '数据合规整改报告', '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"对照个保法完成隐私政策与授权链路整改，管委会督办结案。"}]}]}', '对照个保法完成隐私政策与授权链路整改，管委会督办结案。', NULL, @U, '2026-04-20 10:00:00', 0);

-- ============================ 11. 日历 / 日程 / 参与人 =======================
-- calendar type：personal/meeting/team/resource  visibility：private/busy/public
INSERT INTO pm_calendar (id, tenant_id, name, type, owner_id, color, visibility, is_default, status, create_by, create_time, is_deleted) VALUES
  (9011001, @T, '我的日程', 'personal', @U,      '--mido-color-primary', 'private', 1, 'active', @U, '2026-01-02 09:00:00', 0),
  (9011002, @T, '项目会议', 'meeting',  9002001, '--mido-color-success', 'busy',    0, 'active', @U, '2026-01-02 09:00:00', 0),
  (9011003, @T, '团队日历', 'team',     9002001, '--mido-color-warning', 'public',  0, 'active', @U, '2026-01-02 09:00:00', 0);

-- schedule source_type：manual/task/meeting  status：confirmed/cancelled
INSERT INTO pm_schedule (id, tenant_id, calendar_id, title, description, start_time, end_time, all_day, location, allow_feedback, source_type, organizer_id, status, create_by, create_time, is_deleted) VALUES
  (9011101, @T, 9011002, 'PM 系统周会',      '同步研发进度与风险。',       '2026-06-22 10:00:00', '2026-06-22 11:00:00', 0, '会议室 A',   1, 'meeting', 9002001, 'confirmed', @U, '2026-06-18 09:00:00', 0),
  (9011102, @T, 9011002, '官网改版复盘会',    '上线后转化数据复盘。',       '2026-06-23 14:00:00', '2026-06-23 15:30:00', 0, '会议室 B',   1, 'meeting', 9002010, 'confirmed', @U, '2026-06-18 09:00:00', 0),
  (9011103, @T, 9011002, '渠道招商对接',      '与重点经销商洽谈。',         '2026-06-24 15:00:00', '2026-06-24 16:00:00', 0, '线上腾讯会议', 1, 'meeting', 9002014, 'confirmed', @U, '2026-06-18 09:00:00', 0),
  (9011104, @T, 9011001, 'PRD 终审',         '需求文档定稿评审。',         '2026-06-25 09:30:00', '2026-06-25 11:00:00', 0, '会议室 A',   1, 'manual',  @U,      'confirmed', @U, '2026-06-18 09:00:00', 0),
  (9011105, @T, 9011003, '季度 OKR 对齐',     '全员季度目标对齐。',         '2026-06-26 16:00:00', '2026-06-26 17:30:00', 0, '大会议室',   1, 'manual',  9002001, 'confirmed', @U, '2026-06-18 09:00:00', 0),
  (9011106, @T, 9011003, '团队团建',         '季度团建活动。',             '2026-06-28 00:00:00', '2026-06-28 23:59:59', 1, '待定',       1, 'manual',  9002001, 'confirmed', @U, '2026-06-18 09:00:00', 0),
  (9011107, @T, 9011002, '客服整改验收会',    '整改效果评审。',             '2026-06-29 10:00:00', '2026-06-29 11:00:00', 0, '会议室 C',   1, 'meeting', 9002013, 'confirmed', @U, '2026-06-18 09:00:00', 0),
  (9011108, @T, 9011001, '合规结案归档',      '专项督办结案材料归档。',     '2026-06-30 15:00:00', '2026-06-30 16:00:00', 0, '工位',       0, 'manual',  9002016, 'confirmed', @U, '2026-06-18 09:00:00', 0);

-- 参与人 role：organizer/required/optional  rsvp：pending/accepted/tentative/declined
INSERT INTO pm_schedule_participant (id, tenant_id, schedule_id, user_id, external_name, role, rsvp_status, is_deleted) VALUES
  (9011201, @T, 9011101, 9002001, NULL, 'organizer', 'accepted',  0),
  (9011202, @T, 9011101, 9002003, NULL, 'required',  'accepted',  0),
  (9011203, @T, 9011101, 9002007, NULL, 'required',  'tentative', 0),
  (9011204, @T, 9011101, 9002009, NULL, 'optional',  'pending',   0),
  (9011205, @T, 9011102, 9002010, NULL, 'organizer', 'accepted',  0),
  (9011206, @T, 9011102, 9002011, NULL, 'required',  'accepted',  0),
  (9011207, @T, 9011102, 9002006, NULL, 'required',  'declined',  0),
  (9011208, @T, 9011103, 9002014, NULL, 'organizer', 'accepted',  0),
  (9011209, @T, 9011103, NULL, '重点经销商', 'required', 'pending', 0),
  (9011210, @T, 9011105, 9002001, NULL, 'organizer', 'accepted',  0);

-- ============================ 12. 简报（日/周/月报） =========================
-- template type：daily/weekly/monthly
INSERT INTO pm_briefing_template (id, tenant_id, name, type, schema_def, scope, is_builtin, status, create_by, create_time, is_deleted) VALUES
  (9012001, @T, '研发日报', 'daily',   '[{"key":"done","label":"今日完成","type":"textarea"},{"key":"plan","label":"明日计划","type":"textarea"},{"key":"risk","label":"风险/阻塞","type":"textarea"}]', 'tenant', 1, 'active', @U, '2026-01-02 09:00:00', 0),
  (9012002, @T, '项目周报', 'weekly',  '[{"key":"progress","label":"本周进展","type":"textarea"},{"key":"next","label":"下周计划","type":"textarea"},{"key":"issue","label":"问题与需协调","type":"textarea"}]', 'tenant', 1, 'active', @U, '2026-01-02 09:00:00', 0),
  (9012003, @T, '经营月报', 'monthly', '[{"key":"summary","label":"经营概览","type":"textarea"},{"key":"kpi","label":"关键指标","type":"textarea"},{"key":"plan","label":"下月重点","type":"textarea"}]', 'tenant', 1, 'active', @U, '2026-01-02 09:00:00', 0);

-- 简报实例 status：draft/submitted
INSERT INTO pm_briefing (id, tenant_id, template_id, type, author_id, dept_id, period_key, period_start, period_end, content, status, submitted_at, create_by, create_time, is_deleted) VALUES
  (9012101, @T, 9012001, 'daily',   9002007, 9001006, '2026-06-25', '2026-06-25', '2026-06-25', '{"done":"完成任务接口联调","plan":"看板拖拽收尾","risk":"无"}', 'submitted', '2026-06-25 18:30:00', 9002007, '2026-06-25 18:00:00', 0),
  (9012102, @T, 9012001, 'daily',   9002002, 9001005, '2026-06-25', '2026-06-25', '2026-06-25', '{"done":"工作台聚合视图","plan":"项目列表筛选","risk":"等后端接口"}', 'submitted', '2026-06-25 18:40:00', 9002002, '2026-06-25 18:00:00', 0),
  (9012103, @T, 9012001, 'daily',   9002005, 9001005, '2026-06-25', '2026-06-25', '2026-06-25', '{"done":"看板拖拽组件 80%","plan":"联调状态流转","risk":"无"}', 'draft', NULL, 9002005, '2026-06-25 19:00:00', 0),
  (9012104, @T, 9012002, 'weekly',  9002001, 9001001, '2026-W26', '2026-06-22', '2026-06-28', '{"progress":"核心模块开发推进至 67%","next":"启动 NPSS 引擎","issue":"测试人力偏紧"}', 'submitted', '2026-06-26 17:00:00', 9002001, '2026-06-26 16:00:00', 0),
  (9012105, @T, 9012002, 'weekly',  9002012, 9001003, '2026-W26', '2026-06-22', '2026-06-28', '{"progress":"经销商签约 9 家","next":"招商会筹备","issue":"部分区域物料未到位"}', 'submitted', '2026-06-26 17:20:00', 9002012, '2026-06-26 16:00:00', 0),
  (9012106, @T, 9012002, 'weekly',  9002013, 9001003, '2026-W26', '2026-06-22', '2026-06-28', '{"progress":"客服整改进入验收","next":"复测客诉率","issue":"无"}', 'submitted', '2026-06-26 17:40:00', 9002013, '2026-06-26 16:00:00', 0),
  (9012107, @T, 9012003, 'monthly', 9002010, 9001002, '2026-06', '2026-06-01', '2026-06-30', '{"summary":"官网改版上线，转化率达 4.3%","kpi":"新签 2150 万 / 转化 4.3%","plan":"渠道拓展加速"}', 'submitted', '2026-06-26 18:00:00', 9002010, '2026-06-26 16:00:00', 0),
  (9012108, @T, 9012003, 'monthly', 9002001, 9001001, '2026-05', '2026-05-01', '2026-05-31', '{"summary":"PM 系统进入开发中期","kpi":"模块上线 8/12","plan":"NPSS 与报表"}', 'submitted', '2026-06-01 10:00:00', 9002001, '2026-06-01 09:00:00', 0);

-- 简报评审人/抄送 type：reviewer/cc
INSERT INTO pm_briefing_recipient (id, tenant_id, briefing_id, user_id, type, is_deleted) VALUES
  (9012201, @T, 9012104, 9002001, 'reviewer', 0),
  (9012202, @T, 9012104, 9002010, 'cc',       0),
  (9012203, @T, 9012105, 9002012, 'reviewer', 0),
  (9012204, @T, 9012107, 9002001, 'reviewer', 0),
  (9012205, @T, 9012101, 9002003, 'reviewer', 0);

-- ============================ 13. 协作：评论 / 通知 =========================
-- comment entity_type：task/project/goal
INSERT INTO pm_comment (id, tenant_id, entity_type, entity_id, user_id, content, mention, create_time, is_deleted) VALUES
  (9013001, @T, 'task',    9004003, 9002001, '接口进度不错，注意多租户拦截器的覆盖测试。', '[9002007]', '2026-03-05 10:00:00', 0),
  (9013002, @T, 'task',    9004003, 9002007, '收到，已补充 tenant_id 注入用例。', NULL, '2026-03-05 11:00:00', 0),
  (9013003, @T, 'task',    9004004, 9002002, '工作台聚合接口字段能否一次返回？', '[9002007]', '2026-03-06 14:00:00', 0),
  (9013004, @T, 'project', 9003002, 9002010, '官网转化率已到 4.3%，继续观察一周。', NULL, '2026-05-22 09:00:00', 0),
  (9013005, @T, 'project', 9003003, 9002012, '招商会预算请财务复核。', '[9002015]', '2026-05-12 16:00:00', 0),
  (9013006, @T, 'goal',    9005002, 9002012, '上半年增长超预期，建议上调目标。', NULL, '2026-05-08 09:00:00', 0),
  (9013007, @T, 'task',    9004033, 9002013, '客诉率已降至目标线以下，准备验收。', NULL, '2026-06-20 10:00:00', 0),
  (9013008, @T, 'project', 9003001, 9002001, '整体节奏可控，NPSS 引擎是下阶段重点。', NULL, '2026-06-10 09:00:00', 0);

-- 通知（is_read 混合；type 取自 NotificationListener 事件名）
INSERT INTO pm_notification (id, tenant_id, user_id, type, biz_type, biz_id, title, link, is_read, channel, create_time, is_deleted) VALUES
  (9014001, @T, 9002007, 'task.assigned',       'task',     9004003, '任务指派：项目/任务模块后端', '/project/9003001/task/9004003', 1, 'inapp', '2026-02-11 10:05:00', 0),
  (9014002, @T, 9002008, 'task.assigned',       'task',     9004005, '任务指派：NPSS 验收引擎',     '/project/9003001/task/9004005', 0, 'inapp', '2026-04-25 10:05:00', 0),
  (9014003, @T, 9002007, 'comment.created',     'task',     9004003, '有人在任务中@了你',           '/project/9003001/task/9004003', 0, 'inapp', '2026-03-05 10:01:00', 0),
  (9014004, @T, 11,      'approval.submitted',  'approval', 9008001, '待审批：移动端 App 一期立项',  '/approval?open=9008001',        0, 'inapp', '2026-06-20 09:31:00', 0),
  (9014005, @T, 9002010, 'approval.approved',   'approval', 9008002, '立项审批通过：品牌官网改版',   '/approval?open=9008002',        1, 'inapp', '2026-01-30 16:05:00', 0),
  (9014006, @T, 9002015, 'comment.created',     'project',  9003003, '有人在项目中@了你',           '/project/9003003',              0, 'inapp', '2026-05-12 16:01:00', 0),
  (9014007, @T, 9002010, 'npss.review.started', 'project',  9003002, 'NPSS 评分已发起：官网改版',    '/project/9003002',              1, 'inapp', '2026-05-25 09:05:00', 0),
  (9014008, @T, 9002012, 'project.budget.exceeded','project',9003003,'预算预警：华东区渠道拓展',     '/project/9003003',              0, 'inapp', '2026-05-16 09:00:00', 0),
  (9014009, @T, 9002001, 'briefing.submitted',  'briefing', 9012104, '简报待评审：项目周报',         '/briefing',                     0, 'inapp', '2026-06-26 17:05:00', 0),
  (9014010, @T, 9002007, 'briefing.reviewed',   'briefing', 9012101, '你的日报有新的评审',           '/briefing',                     1, 'inapp', '2026-06-25 19:30:00', 0);

-- ============================ 14. 平台运营后台（演示租户/订阅/收入/公告） =====
-- 平台域全局表（无 tenant_id）。这些演示租户仅用于运营后台列表展示，不含业务数据。
-- status：trial/active/suspended/expired/closed
INSERT INTO sys_tenant (id, code, name, status, industry, contact_name, contact_phone, contact_email, source, activated_at, expire_at, remark, is_deleted) VALUES
  (9020001, 'acme',     '艾可科技',     'active',    '互联网',   '张总', '13900020001', 'zhang@acme.com',   'manual', '2026-01-10 09:00:00', '2027-01-09 23:59:59', '旗舰版年付', 0),
  (9020002, 'sunshine', '阳光教育',     'trial',     '教育',     '李总', '13900020002', 'li@sunshine.com',  'manual', '2026-06-01 09:00:00', '2026-06-30 23:59:59', '试用中',     0),
  (9020003, 'oceanic',  '远洋物流',     'active',    '物流',     '王总', '13900020003', 'wang@oceanic.com', 'manual', '2026-03-15 09:00:00', '2027-03-14 23:59:59', '标准版',     0),
  (9020004, 'meadow',   '青禾农业',     'suspended', '农业',     '赵总', '13900020004', 'zhao@meadow.com',  'manual', '2025-11-01 09:00:00', '2026-04-30 23:59:59', '欠费停用',   0),
  (9020005, 'pioneer',  '先锋制造',     'expired',   '制造',     '陈总', '13900020005', 'chen@pioneer.com', 'manual', '2025-05-01 09:00:00', '2026-04-30 23:59:59', '已到期待续', 0);

-- 订阅（plan_id 复用内置 1=free / 2=standard / 3=flagship）status：active/expired/cancelled
INSERT INTO sys_tenant_subscription (id, tenant_id, plan_id, start_at, expire_at, status, remark, is_deleted) VALUES
  (9020101, 9020001, 3, '2026-01-10 00:00:00', '2027-01-09 23:59:59', 'active',   '旗舰版年付', 0),
  (9020102, 9020002, 1, '2026-06-01 00:00:00', '2026-06-30 23:59:59', 'active',   '试用',       0),
  (9020103, 9020003, 2, '2026-03-15 00:00:00', '2027-03-14 23:59:59', 'active',   '标准版年付', 0),
  (9020104, 9020004, 2, '2025-11-01 00:00:00', '2026-04-30 23:59:59', 'expired',  '欠费',       0),
  (9020105, 9020005, 2, '2025-05-01 00:00:00', '2026-04-30 23:59:59', 'expired',  '到期',       0);

-- 收入台账 type：payment/refund
INSERT INTO sys_revenue_record (id, tenant_id, type, amount, contract_no, occurred_date, remark, is_deleted) VALUES
  (9020201, 9020001, 'payment', 98000.00,  'HT-2026-0110', '2026-01-10', '旗舰版年费', 0),
  (9020202, 9020003, 'payment', 36000.00,  'HT-2026-0315', '2026-03-15', '标准版年费', 0),
  (9020203, 9020005, 'payment', 36000.00,  'HT-2025-0501', '2025-05-01', '标准版年费', 0),
  (9020204, 9020004, 'payment', 36000.00,  'HT-2025-1101', '2025-11-01', '标准版年费', 0),
  (9020205, 9020005, 'refund',  -6000.00,  'HT-2025-0501', '2026-05-05', '到期未续部分退款', 0),
  (9020206, 9020001, 'payment', 12000.00,  'HT-2026-0210', '2026-02-10', '增购席位', 0),
  (9020207, 9020003, 'payment', 8000.00,   'HT-2026-0401', '2026-04-01', '存储扩容', 0);

-- 平台公告 level：info/warning  status：draft/published
INSERT INTO sys_announcement (id, title, content, level, status, publish_at, expire_at, is_deleted) VALUES
  (9020301, '系统升级通知', '平台将于本周日 02:00-04:00 进行例行维护，期间服务短暂不可用。', 'warning', 'published', '2026-06-20 09:00:00', '2026-06-30 23:59:59', 0),
  (9020302, 'v1.2 版本发布', '新增简报评审、日历资源预订能力，欢迎体验。', 'info', 'published', '2026-06-10 09:00:00', NULL, 0),
  (9020303, '企微集成上线预告', '企业微信通讯录同步、消息推送将于下个版本开放。', 'info', 'draft', NULL, NULL, 0);

-- 租户用量快照（内置租户 1，运营概览用）resource：user/project/task/storage_mb
-- 唯一键 (tenant_id, resource)：运行时可能已存在快照，用 upsert 避免冲突
INSERT INTO sys_tenant_quota_usage (id, tenant_id, resource, used_value, snapshot_time, is_deleted) VALUES
  (9020401, 1, 'user',       17,   '2026-06-26 00:00:00', 0),
  (9020402, 1, 'project',    6,    '2026-06-26 00:00:00', 0),
  (9020403, 1, 'task',       22,   '2026-06-26 00:00:00', 0),
  (9020404, 1, 'storage_mb', 1280, '2026-06-26 00:00:00', 0)
ON DUPLICATE KEY UPDATE used_value = VALUES(used_value), snapshot_time = VALUES(snapshot_time), is_deleted = 0;

-- =============================== 演示数据结束 ================================
