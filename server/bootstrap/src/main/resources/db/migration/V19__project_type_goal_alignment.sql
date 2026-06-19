-- V19 项目类型差异化对齐策略：新增 require_goal_alignment——该类型立项时是否强制已对齐 OKR。
-- 战略级 S 默认强制（项目须挂到公司目标之下方可立项）；I/O 默认不强制。租户可改。
-- 与「目标弱关联」一脉相承：不改变弱关联本质，仅在立项 guard 处按类型施加对齐约束。

ALTER TABLE pm_project_type
  ADD COLUMN require_goal_alignment TINYINT DEFAULT 0 COMMENT '立项是否强制已对齐目标 1是/0否' AFTER default_flow_id;

-- 内置种子：S 强制对齐，其余保持 0（V17 已插入 5 条类型）
UPDATE pm_project_type SET require_goal_alignment = 1 WHERE code = 'S' AND tenant_id = 1;
