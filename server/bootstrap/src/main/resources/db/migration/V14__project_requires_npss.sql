-- V14 NPSS 解耦：项目增加「是否走价值验收(NPSS)」开关，结束「所有项目结案后被无条件唤醒 NPSS」的硬耦合。
-- 默认 1=走 NPSS；仅 O·定向整改 / O·专项督办 默认 0=不走（它们本就无项目奖金，铁三角结果验收闭环即可）。
-- 非 NPSS 项目结案不安排 value_review_due_date，定时任务自然不会唤醒其价值验收。

ALTER TABLE pm_project
  ADD COLUMN requires_npss TINYINT DEFAULT 1 COMMENT '是否走NPSS价值验收 1是/0否' AFTER value_review_due_date;

-- 回填存量项目：O·定向整改/专项督办 置 0，其余保持默认 1
UPDATE pm_project
  SET requires_npss = 0
  WHERE category = 'O' AND sub_category IN ('定向整改', '专项督办');
