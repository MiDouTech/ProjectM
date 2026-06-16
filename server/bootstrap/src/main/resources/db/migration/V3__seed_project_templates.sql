-- V3 内置项目模板种子（is_builtin=1，租户 1）。
-- 来源：architecture-overview §3（阶段/审批流/验收）+ npss-rule §6（默认干系人权重）。
-- config 为固定 schema 的 JSON：phases[] / stakeholders[] / approvalFlow / verifyMethod。
-- 注：专项督办按 §6 受益方(业务)权重为 20，与 §4「受益方≥50%」存在源文档张力，
--     以 §6 为模板默认；正式校验/调整在干系人域(Step 5)落地，PMO 可微调。

INSERT INTO pm_project_template (id, tenant_id, name, category, sub_category, description, is_builtin, config, is_deleted)
VALUES (1, 1, '战略级 S 标准', 'S', NULL, 'IMP/MAP/EBC 等年度重点', 1,
'{"phases":[{"name":"立项","tasks":["填写立项申请","干系人初稿"]},{"name":"规划","tasks":["制定项目计划","干系人权重对齐"]},{"name":"执行","tasks":["执行任务","月度复盘"]},{"name":"结果验收","tasks":["铁三角验收"]},{"name":"年度结案","tasks":["结案归档"]},{"name":"NPSS","tasks":["价值验收(延后)"]}],"stakeholders":[{"role":"sponsor","weight":30},{"role":"business","weight":30},{"role":"team","weight":10},{"role":"finance","weight":10},{"role":"other","weight":20}],"approvalFlow":"S_STANDARD","verifyMethod":"铁三角+NPSS"}',
0);

INSERT INTO pm_project_template (id, tenant_id, name, category, sub_category, description, is_builtin, config, is_deleted)
VALUES (2, 1, '创新级 I · POC', 'I', NULL, '一米宽十米深探索/MTS 类', 1,
'{"phases":[{"name":"假设","tasks":["提出价值假设"]},{"name":"POC设计","tasks":["设计验证方案"]},{"name":"验证","tasks":["执行POC","收集数据"]},{"name":"复盘","tasks":["复盘结论"]}],"stakeholders":[{"role":"sponsor","weight":30},{"role":"business","weight":30},{"role":"team","weight":10},{"role":"finance","weight":10},{"role":"other","weight":20}],"approvalFlow":"I_POC","verifyMethod":"铁三角+NPSS"}',
0);

INSERT INTO pm_project_template (id, tenant_id, name, category, sub_category, description, is_builtin, config, is_deleted)
VALUES (3, 1, '运营级 O · 常规运营', 'O', '常规运营', '米多星球/PDA 改造等攻坚', 1,
'{"phases":[{"name":"立项","tasks":["填写立项申请"]},{"name":"执行","tasks":["执行任务"]},{"name":"结果验收","tasks":["铁三角验收"]},{"name":"NPSS","tasks":["价值验收(延后)"]}],"stakeholders":[{"role":"business","weight":50},{"role":"management","weight":20},{"role":"team","weight":10},{"role":"finance","weight":10},{"role":"other","weight":10}],"approvalFlow":"O_NORMAL","verifyMethod":"铁三角+NPSS"}',
0);

INSERT INTO pm_project_template (id, tenant_id, name, category, sub_category, description, is_builtin, config, is_deleted)
VALUES (4, 1, '运营级 O · 定向整改', 'O', '定向整改', '部门月度问题及对策转化', 1,
'{"phases":[{"name":"问题确认","tasks":["确认问题与对策"]},{"name":"整改","tasks":["执行整改"]},{"name":"验收","tasks":["整改验收"]}],"stakeholders":[{"role":"business","weight":50},{"role":"management","weight":20},{"role":"team","weight":10},{"role":"finance","weight":10},{"role":"other","weight":10}],"approvalFlow":"O_RECTIFY","verifyMethod":"铁三角(无奖金)"}',
0);

INSERT INTO pm_project_template (id, tenant_id, name, category, sub_category, description, is_builtin, config, is_deleted)
VALUES (5, 1, '运营级 O · 专项督办', 'O', '专项督办', '管委会基础素养处分转化', 1,
'{"phases":[{"name":"立案","tasks":["立案登记"]},{"name":"根治","tasks":["根因治理"]},{"name":"督办验收","tasks":["督办验收"]}],"stakeholders":[{"role":"regulator","weight":30},{"role":"management","weight":30},{"role":"business","weight":20},{"role":"team","weight":10},{"role":"other","weight":10}],"approvalFlow":"O_SUPERVISE","verifyMethod":"铁三角(无奖金)"}',
0);
