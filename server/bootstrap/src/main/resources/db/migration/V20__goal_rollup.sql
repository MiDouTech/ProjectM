-- V20 目标-项目进度自动汇总（G1）：
--   pm_goal_alignment.weight —— 对齐贡献权重（多项目汇总到一个 KR 时按权重加权，默认 1=等权）
--   pm_goal.auto_rollup     —— 该 KR 进度是否由"对齐项目的任务完成率"自动汇总反写（默认 0=关，手动）
-- 口径（产品决策）：项目进度=已完成任务/总任务；KR 进度=各对齐项目完成率按 weight 加权平均，
--   再按 KR 量纲 current = start + 加权完成率% × (target-start) 反写。仅 auto_rollup=1 的量化 KR 生效。

ALTER TABLE pm_goal_alignment
  ADD COLUMN weight DECIMAL(5,2) DEFAULT 1 COMMENT '对齐贡献权重(加权汇总用)' AFTER target_id;

ALTER TABLE pm_goal
  ADD COLUMN auto_rollup TINYINT DEFAULT 0 COMMENT 'KR进度是否自动汇总对齐项目完成率 1是/0否' AFTER progress;
